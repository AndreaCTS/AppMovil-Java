package com.example.appmovil.ui.parking;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.appmovil.databinding.FragmentParkingBinding;
import com.example.appmovil.ui.rfid.DataManager;
import com.example.appmovil.ui.wifi.WifiHandler;

public class ParkingFragment extends Fragment {

    private FragmentParkingBinding binding;
    private ParkingMap parkingMap;
    private WifiHandler wifiHandler;
    private Handler handler;
    private Runnable refreshTask;
    private static final int REFRESH_INTERVAL = 1000;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentParkingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializa el mapa de parqueaderos
        ImageView[] parkingSpots = {
                binding.spot1, binding.spot2, binding.spot3, binding.spot4, binding.spot5,
                binding.spot6, binding.spot7, binding.spot8
        };
        parkingMap = new ParkingMap(parkingSpots);

        // Configura el Wi-Fi para recibir datos
        wifiHandler = new WifiHandler("192.168.4.1/parkingData"); // Cambia esta IP
        // Configura la tarea de actualización periódica
        handler = new Handler();
        refreshTask = new Runnable() {
            @Override
            public void run() {
                fetchDataFromServer();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        handler.post(refreshTask); // Inicia la tarea periódica


        return root;
    }

    private void fetchDataFromServer() {
        wifiHandler.fetchData(data -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    String parkingData = DataManager.getInstance().getParkingData();
                    parkingMap.updateMap(parkingData);
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Detén el handler para evitar fugas de memoria
        if (handler != null && refreshTask != null) {
            handler.removeCallbacks(refreshTask);
        }
        binding = null;
    }
}
