package com.example.appmovil.ui.parking;

import java.util.Random;
import android.widget.ImageView;
import com.example.appmovil.R;

public class ParkingMap {

    private Random random = new Random();

    private ImageView[] parkingSpots;

    // Constructor para inicializar los espacios de parqueo
    public ParkingMap(ImageView[] parkingSpots) {
        this.parkingSpots = parkingSpots;
    }

    // Método para actualizar el mapa de parqueaderos basado en los datos recibidos
    public void updateMap(String data) {
        if (data != null && data.length() == parkingSpots.length) {
            for (int i = 0; i < data.length(); i++) {
                if (data.charAt(i) == '1') {
                    int randomIndex = random.nextInt(3);
                    switch (randomIndex) {
                        case 0:
                            parkingSpots[i].setImageResource(R.drawable.espacio_ocupado_1);
                            break;
                        case 1:
                            parkingSpots[i].setImageResource(R.drawable.espacio_ocupado_2);
                            break;
                        case 2:
                            parkingSpots[i].setImageResource(R.drawable.espacio_ocupado_3);
                            break;
                    }

                } else {
                    parkingSpots[i].setImageResource(R.drawable.espacio_desocupado);
                }
            }
        }
    }
}
