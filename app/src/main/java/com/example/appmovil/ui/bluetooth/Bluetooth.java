package com.example.appmovil.ui.bluetooth;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Bluetooth{

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String deviceName;
    private UUID uuid;

    private OnDataReceivedListener dataReceivedListener;

    // device name es como se llama en el bluetooth
    public Bluetooth(String deviceName) {
        this.deviceName = deviceName;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    public void connect() throws Exception {
        BluetoothDevice device = findDeviceByName(deviceName);
        if (device == null) {
            throw new Exception("Dispositivo Bluetooth no encontrado: " + deviceName);
        }
        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
        bluetoothSocket.connect();
        inputStream = bluetoothSocket.getInputStream();
        outputStream = bluetoothSocket.getOutputStream();
    }

    @SuppressLint("MissingPermission")
    private BluetoothDevice findDeviceByName(String name) {
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (name.equals(device.getName())) {
                return device;
            }
        }
        return null;
    }

    public void startListening() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            try {
                while ((bytes = inputStream.read(buffer)) != -1) {
                    String receivedData = new String(buffer, 0, bytes);
                    if (dataReceivedListener != null) {
                        dataReceivedListener.onDataReceived(receivedData);
                    }
                }
            } catch (Exception e) {
                if (dataReceivedListener != null) {
                    dataReceivedListener.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void sendData(String data) throws Exception {
        if (outputStream != null) {
            outputStream.write(data.getBytes());
        } else {
            throw new Exception("No hay conexión Bluetooth activa.");
        }
    }

    public void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (Exception ignored) {
        }
    }

    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        this.dataReceivedListener = listener;
    }

    public interface OnDataReceivedListener {
        void onDataReceived(String data);

        void onError(String error);
    }
}