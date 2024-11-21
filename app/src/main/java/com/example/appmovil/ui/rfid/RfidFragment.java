package com.example.appmovil.ui.rfid;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.appmovil.R;
import com.example.appmovil.databinding.FragmentRfidBinding;
import com.example.appmovil.ui.carros.Vehiculo;
import com.example.appmovil.ui.wifi.WifiHandler;

import java.util.ArrayList;
import java.util.Date;

public class RfidFragment extends Fragment {

    private FragmentRfidBinding binding;
    private WifiHandler wifiHandler; // Instancia para manejar conexión Wi-Fi
    private TextView textView; // TextView para mostrar mensajes
    private ArrayList<Vehiculo> vehiculos; // Lista de vehículos en el parqueadero
    private LinearLayout vehicleContainer; // Contenedor para mostrar vehículos en pantalla

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRfidBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = binding.textHome;
        this.vehiculos = new ArrayList<>();
        vehicleContainer = root.findViewById(R.id.vehicle_container);

        // Inicializar WifiHandler con la URL del servidor
        wifiHandler = new WifiHandler("192.168.0.10"); // Cambiar por la IP/puerto del servidor

        // Iniciar la escucha de datos
        fetchDataFromServer();

        return root;
    }

    private void fetchDataFromServer() {
        wifiHandler.fetchData(data -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    String rfidData = DataManager.getInstance().getRfidData();
                    gestionParqueaderos(rfidData);
                });
            }
        });
    }


    private void updateVehicleDisplay() {
        vehicleContainer.removeAllViews();
        if (!vehiculos.isEmpty()) {
            for (Vehiculo vehiculo : vehiculos) {
                // Crear cuadro para cada vehículo
                TextView vehicleView = new TextView(getContext());
                vehicleView.setText("ID: " + vehiculo.getUid() + "\nPlaca: " + vehiculo.getPlaca() + "\nEntrada: " + vehiculo.getHoraEntrada());
                vehicleView.setPadding(16, 16, 16, 16);
                vehicleView.setBackgroundColor(Color.parseColor("#DDDDDD"));
                vehicleView.setTextColor(Color.BLACK);
                vehicleView.setGravity(Gravity.CENTER_VERTICAL);
                vehicleView.setTextSize(16);

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

    public void gestionParqueaderos(String data) {
        data = data.trim();
        Vehiculo vehiculoSaliente = null;

        if (!vehiculos.isEmpty()) {
            for (int i = 0; i < vehiculos.size(); i++) {
                if (vehiculos.get(i).getUid().equals(data)) {
                    vehiculos.get(i).setHoraSalida(new Date());
                    vehiculoSaliente = vehiculos.get(i);
                    vehiculos.remove(i);
                    break;
                }
            }
        }

        if (vehiculoSaliente == null) {
            boolean condicion = (data == null || data.trim().isEmpty());

            if (vehiculos.size() <= 8 && !condicion) {
                Vehiculo v = new Vehiculo("nnn000", data);
                vehiculos.add(v);
            }
        } else {
            mostrarNotificacionSalida(vehiculoSaliente);
        }
        updateVehicleDisplay();
    }

    private void mostrarNotificacionSalida(Vehiculo vehiculo) {
        Date horaEntrada = vehiculo.getHoraEntrada();
        Date horaSalida = vehiculo.getHoraSalida();
        long duracion = vehiculo.calcularTiempoEnParqueadero();
        long minutos = (duracion / (1000 * 60)) % 60;
        long horas = (duracion / (1000 * 60 * 60));

        String mensaje = "Placa: " + vehiculo.getPlaca() + "\n" +
                "Hora de entrada: " + horaEntrada + "\n" +
                "Hora de salida: " + horaSalida + "\n" +
                "Duración: " + horas + " horas y " + minutos + " minutos.";

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Vehículo salió del parqueadero")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
