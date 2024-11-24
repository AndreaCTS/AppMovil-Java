package com.example.appmovil.ui.rfid;

public class DataManager {
    private static DataManager instance;
    private String rfidData;
    private String parkingData;

    private DataManager() {}

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public void processData(String data) {
        // Suponiendo formato "RFID:10010001;PARKING:01010101"
        String[] parts = data.split(";");
        for (String part : parts) {
            if (part.startsWith("RFID:")) {
                rfidData = part.substring(5);
            } else if (part.startsWith("PARKING:")) {
                parkingData = part.substring(8);
            }
        }
    }

    public String getRfidData() {
        return rfidData;
    }

    public String getParkingData() {
        return parkingData;
    }
}
