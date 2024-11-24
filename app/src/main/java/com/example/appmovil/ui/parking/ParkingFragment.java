package com.example.appmovil.ui.parking;

import android.os.Bundle;
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
        wifiHandler = new WifiHandler("192.168.0.10"); // Cambia esta IP
        wifiHandler.fetchData(data -> {
            requireActivity().runOnUiThread(() -> {
                String parkingData = DataManager.getInstance().getParkingData();
                parkingMap.updateMap(parkingData);
            });
        });


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
