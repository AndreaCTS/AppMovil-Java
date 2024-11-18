package com.example.appmovil.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.appmovil.databinding.FragmentHomeBinding;
import com.example.appmovil.ui.bluetooth.Bluetooth;

public class HomeFragment extends Fragment {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private FragmentHomeBinding binding;
    private Bluetooth bluetooth; // Instancia de la clase Bluetooth
    private TextView textView; // TextView para mostrar los datos recibidos

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializar el TextView para mostrar los datos
        textView = binding.textHome; // Asumiendo que tienes un TextView en tu layout con id textHome

        // Inicialización del Bluetooth
        bluetooth = new Bluetooth("ESP32_Parqueadero");  // Cambia el nombre del dispositivo Bluetooth si es necesario

        // Verificar si Bluetooth está disponible y habilitado
        if (!bluetooth.isBluetoothAvailable()) {
            textView.setText("Bluetooth no está disponible en este dispositivo.");
        } else if (!bluetooth.isBluetoothEnabled()) {
            textView.setText("Por favor, habilita Bluetooth.");
        } else {
            // Verificar permisos y solicitar si es necesario
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
                } else {
                    // Si ya tenemos permisos, intentar la conexión
                    connectToBluetooth();
                }
            } else {
                // En versiones anteriores a Android 12, no es necesario solicitar permisos específicos de Bluetooth
                connectToBluetooth();
            }
        }

        return root;
    }

    private void connectToBluetooth() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetooth.connect(); // Intentamos conectar
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Conectado al dispositivo Bluetooth.");
                        }
                    });

                    bluetooth.startListening(); // Empezamos a escuchar los datos enviados por el ESP32
                } catch (Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Error al conectar: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();

        // Configuración para recibir datos de Bluetooth
        bluetooth.setOnDataReceivedListener(new Bluetooth.OnDataReceivedListener() {
            @Override
            public void onDataReceived(String data) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Muestra los datos recibidos en el TextView
                        textView.append("\n" + data);
                    }
                });
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Si ocurre un error, lo mostramos en un Toast
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, intentamos la conexión
                connectToBluetooth();
            } else {
                // Permiso denegado, muestra un mensaje de advertencia
                Toast.makeText(getContext(), "Permiso denegado. No se puede acceder al Bluetooth.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        // Cerrar la conexión Bluetooth al destruir el fragmento
        if (bluetooth != null) {
            bluetooth.closeConnection();
        }
    }
}
