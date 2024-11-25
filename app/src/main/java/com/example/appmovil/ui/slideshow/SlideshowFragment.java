package com.example.appmovil.ui.slideshow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.appmovil.databinding.FragmentSlideshowBinding;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class SlideshowFragment extends Fragment {
    private FragmentSlideshowBinding binding;
    private ImageView imageView;
    private static final String ESP32_URL = "https://drive.google.com/uc?export=view&id=188k1ooINJm4fuTutsIc22JxSW_L6sEWy";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        imageView = binding.imageView;

        // Inicia el procesamiento en segundo plano
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            Bitmap bitmap = fetchImage(ESP32_URL);
            if (bitmap != null) {
                Bitmap processedBitmap = processImage(bitmap);
                // Actualiza la UI en el hilo principal
                getActivity().runOnUiThread(() -> imageView.setImageBitmap(processedBitmap));
            }
        });

        return root;
    }

    private Bitmap fetchImage(String urlString) {
        Bitmap bitmap = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            } else {
                Log.e("ImageDownload", "Error: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            Log.e("ImageDownload", "Error: " + e.getMessage(), e);
        }
        return bitmap;
    }

    private Bitmap processImage(Bitmap bitmap) {
        try {
            // Convertir el Bitmap a Mat
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);

            // Validar que el Mat no esté vacío
            if (mat.empty()) {
                Log.e("ProcessImage", "El Mat está vacío después de la conversión");
                return null;
            }

            Mat hsvMat = new Mat();
            Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV);

            // Definir un rango para el color amarillo en HSV
            // Ajuste para más saturación y valor
            Scalar lowerYellow = new Scalar(15, 120, 100); // Satura un poco más el color
            Scalar upperYellow = new Scalar(45, 255, 255); // Rango más amplio para captar más amarillos

            // Crear una máscara para los píxeles amarillos
            Mat yellowMask = new Mat();
            Core.inRange(hsvMat, lowerYellow, upperYellow, yellowMask);

            // Verifica si la máscara tiene píxeles blancos
            int nonZeroCount = Core.countNonZero(yellowMask);
            Log.d("ProcessImage", "Píxeles detectados en la máscara amarilla: " + nonZeroCount);

            if (nonZeroCount == 0) {
                Log.e("ProcessImage", "No se detectaron áreas amarillas.");
            }

            // Validar que la máscara no esté vacía
            if (yellowMask.empty()) {
                Log.e("ProcessImage", "La máscara amarilla está vacía");
                return null;
            }

            // Aplicar la máscara a la imagen original para extraer la región amarilla
            Mat yellowRegion = new Mat();
            Core.bitwise_and(mat, mat, yellowRegion, yellowMask);

            // Convertir la imagen a escala de grises para la detección de contornos
            Mat grayMat = new Mat();
            Imgproc.cvtColor(yellowRegion, grayMat, Imgproc.COLOR_RGB2GRAY);

            // Aplicar un umbral para obtener una imagen binaria
            Mat thresholdMat = new Mat();
            Imgproc.threshold(grayMat, thresholdMat, 120, 255, Imgproc.THRESH_BINARY);

            // Detectar los contornos
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(thresholdMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Dibujar contornos y filtrar regiones con proporciones específicas
            for (MatOfPoint contour : contours) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) boundingRect.width / boundingRect.height;
                // Comprobar si el aspecto corresponde a una placa (relación de aspecto típica entre 2 y 4)
                if (aspectRatio > 1 && aspectRatio < 5) {
                    System.out.println("Entraaaaa");
                    // Dibujar un rectángulo alrededor de la posible placa
                    Imgproc.rectangle(mat, boundingRect.tl(), boundingRect.br(), new Scalar(0, 255, 0), 2);
                }
            }

            // Convertir el Mat procesado de vuelta a Bitmap
            Bitmap processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, processedBitmap);

            //Bitmap yellowMaskBitmap = Bitmap.createBitmap(yellowMask.cols(), yellowMask.rows(), Bitmap.Config.ARGB_8888);
            //Utils.matToBitmap(yellowMask, yellowMaskBitmap);
            return processedBitmap;  // Devuelve la máscara en lugar de la imagen original

        } catch (Exception e) {
            Log.e("ProcessImage", "Error procesando la imagen: " + e.getMessage(), e);
            return null;
        }
    }


}
