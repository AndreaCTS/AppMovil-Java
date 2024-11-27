package com.example.appmovil.ui.wifi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.example.appmovil.ui.rfid.DataManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WifiHandler {

    private static String serverUrl;

    public WifiHandler(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public interface OnImageReceivedListener {
        void onImageReceived(Bitmap bitmap);
    }

    public void fetchData(OnDataReceivedListener listener) {
        new FetchDataTask(listener).execute();
    }

    // Nuevo método para obtener imágenes
    public void fetchImage(OnImageReceivedListener listener) {
        new FetchImageTask(listener).execute();
    }

    public interface OnDataReceivedListener {
        void onDataReceived(String data);
    }

    // Tarea para descargar imágenes
    private static class FetchImageTask extends AsyncTask<Void, Void, Bitmap> {
        private OnImageReceivedListener listener;

        FetchImageTask(OnImageReceivedListener listener) {
            this.listener = listener;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                URL url = new URL("http://" + serverUrl); // Usa https si es necesario
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                // Obtener el código de respuesta HTTP
                int responseCode = connection.getResponseCode();
                System.out.println("FetchImageTask Código de respuesta: " + responseCode);

                // Verificar si la respuesta es exitosa
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Validar el tipo de contenido
                    String contentType = connection.getHeaderField("Content-Type");
                    System.out.println("FetchImageTask Content-Type: " + contentType);

                    if (contentType == null || !contentType.equals("image/jpeg")) {
                        System.out.println("FetchImageTask El tipo de contenido no es válido: " + contentType);
                        return null;
                    }

                    // Procesar la imagen si el tipo de contenido es válido
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    if (bitmap == null) {
                        System.out.println("FetchImageTask El bitmap es nulo. La respuesta no es una imagen válida.");
                    } else {
                        System.out.println("FetchImageTask Imagen descargada correctamente.");
                    }

                    return bitmap;
                } else {
                    System.out.println("FetchImageTask Error en la respuesta: " + responseCode);
                }
            } catch (Exception e) {
                System.out.println("FetchImageTask Error al descargar la imagen: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (listener != null) {
                listener.onImageReceived(bitmap);
            }
        }
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
            if (data != null && listener != null) {
                // Procesa los datos obtenidos
                DataManager.getInstance().processData(data);

                // Notifica al listener
                listener.onDataReceived(data);
            } else if (listener != null) {
                // Notifica al listener que no se obtuvieron datos
                listener.onDataReceived("No se pudo obtener datos del servidor.");
            }
        }
    }
}