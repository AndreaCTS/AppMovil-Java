package com.example.appmovil.ui.wifi;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WifiHandler {

    private static String serverUrl;

    public WifiHandler(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void fetchData(OnDataReceivedListener listener) {
        //new FetchDataTask(listener).execute();

        String mockData = "10010001";
        listener.onDataReceived(mockData);
    }

    public interface OnDataReceivedListener {
        void onDataReceived(String data);
    }

    private static class FetchDataTask extends AsyncTask<Void, Void, String> {
        private OnDataReceivedListener listener;

        FetchDataTask(OnDataReceivedListener listener) {
            this.listener = listener;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL("http://" + serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                return response.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String data) {
            if (listener != null) {
                listener.onDataReceived(data);
            }
        }
    }
}