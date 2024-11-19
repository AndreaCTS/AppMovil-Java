package com.example.appmovil.ui.bluetooth;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.appmovil.R;
import com.example.appmovil.databinding.FragmentHomeBinding;
import com.example.appmovil.ui.carros.Vehiculo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

public class BluetoothFragment extends Fragment {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private FragmentHomeBinding binding;
    private Bluetooth bluetooth; // Instancia de la clase Bluetooth
    private TextView textView; // TextView para mostrar los datos recibidos

    private ArrayList<Vehiculo> vehiculos;
    private LinearLayout vehicleContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializar el TextView para mostrar los datos
        textView = binding.textHome; // Asumiendo que tienes un TextView en tu layout con id textHome
        this.vehiculos = new ArrayList<>();
        // Inicialización del Bluetooth
        bluetooth = new Bluetooth("S22 Ultra de Laura");  // Cambia el nombre del dispositivo Bluetooth si es necesario

        // Verificar si Bluetooth está disponible y habilitado
        if (!bluetooth.isBluetoothAvailable()) {
            textView.setText("Bluetooth no está disponible en este dispositivo.");
            textView.setVisibility(View.VISIBLE);
        } else if (!bluetooth.isBluetoothEnabled()) {
            textView.setText("Por favor, habilita Bluetooth.");
            textView.setVisibility(View.VISIBLE);
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

        vehicleContainer = root.findViewById(R.id.vehicle_container);
        // Simulación de Bluetooth OnDataReceivedListener
        gestionParqueaderos("ABC123");
        gestionParqueaderos("DEF456");
        gestionParqueaderos("ABC123");
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
                            textView.setVisibility(View.GONE);
                        }
                    });

                    bluetooth.startListening(); // Empezamos a escuchar los datos enviados por el ESP32
                } catch (Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Error al conectar: " + e.getMessage());
                            textView.setVisibility(View.VISIBLE);
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
                        gestionParqueaderos(data);

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

    private void updateVehicleDisplay() {
        // Limpia el contenedor
        vehicleContainer.removeAllViews();
        if(!vehiculos.isEmpty()) {
            for (Vehiculo vehiculo : vehiculos) {
                // Crear cuadro para cada vehículo
                TextView vehicleView = new TextView(getContext());
                vehicleView.setText("ID: " + vehiculo.getUid() +"\nPlaca: " + vehiculo.getPlaca() + "\nEntrada: " + vehiculo.getHoraEntrada());
                vehicleView.setPadding(16, 16, 16, 16);
                vehicleView.setBackgroundColor(Color.parseColor("#DDDDDD"));
                vehicleView.setTextColor(Color.BLACK);
                vehicleView.setGravity(Gravity.CENTER_VERTICAL);
                vehicleView.setTextSize(16);

                // Margen
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 16, 0, 16);
                vehicleView.setLayoutParams(params);

                vehicleContainer.addView(vehicleView);
            }
        }
    }

    public void gestionParqueaderos(String data){
        // Gestión de los datos recibidos
        Vehiculo vehiculoSaliente = null;

        if(!vehiculos.isEmpty()){
            for(int i=0; i<vehiculos.size() ; i++){
                if(vehiculos.get(i).getUid().equals(data)){
                    vehiculos.get(i).setHoraSalida(new Date());
                    vehiculoSaliente = vehiculos.get(i);
                    break;
                }
            }
        }
        if(vehiculoSaliente == null){
            if(vehiculos.size()<=8) {
                Vehiculo v = new Vehiculo("nnn000", data);
                vehiculos.add(v);
            }
        } else {
            // Mostrar notificación de salida
            mostrarNotificacionSalida(vehiculoSaliente);
        }
        updateVehicleDisplay();
    }

    private void mostrarNotificacionSalida(Vehiculo vehiculo) {
        // Calcular la duración de la estadía
        Date horaEntrada = vehiculo.getHoraEntrada();
        Date horaSalida = vehiculo.getHoraSalida();
        long duracion = vehiculo.calcularTiempoEnParqueadero();
        long minutos = (duracion / (1000 * 60)) % 60;
        long horas = (duracion / (1000 * 60 * 60));

        // Formatear el mensaje
        String mensaje = "Placa: " + vehiculo.getPlaca() + "\n" +
                "Hora de entrada: " + horaEntrada + "\n" +
                "Hora de salida: " + horaSalida + "\n" +
                "Duración: " + horas + " horas y " + minutos + " minutos.";

        // Crear y mostrar el cuadro de diálogo
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Vehículo salió del parqueadero")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show();
    }

}
