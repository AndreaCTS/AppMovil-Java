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

import com.example.appmovil.R;
import com.example.appmovil.databinding.FragmentSlideshowBinding;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class SlideshowFragment extends Fragment {
    private FragmentSlideshowBinding binding;
    private ImageView imageView;
    private TessBaseAPI tessBaseAPI;

    private static final String ESP32_URL = "https://drive.google.com/uc?export=view&id=188k1ooINJm4fuTutsIc22JxSW_L6sEWy";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        imageView = binding.imageView;

        // Copia los datos de Tesseract al sistema de archivos del dispositivo
        prepareTesseractData();

        // Inicializar Tesseract
        String dataPath = getActivity().getFilesDir() + "/tesseract/";
        tessBaseAPI = new TessBaseAPI();
        if (!tessBaseAPI.init(dataPath, "spa")) {
            Log.e("Tesseract", "No se pudo inicializar Tesseract");
        }

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

    private void prepareTesseractData() {
        try {
            // Ruta destino
            String dataPath = getActivity().getFilesDir() + "/tesseract/";
            File tessdataDir = new File(dataPath + "tessdata/");
            if (!tessdataDir.exists()) {
                tessdataDir.mkdirs();
            }

            // Copiar el archivo spa.traineddata si no existe
            File trainedDataFile = new File(tessdataDir, "spa.traineddata");
            if (!trainedDataFile.exists()) {
                InputStream inputStream = getActivity().getAssets().open("tessdata/spa.traineddata");
                OutputStream outputStream = new FileOutputStream(trainedDataFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                Log.d("Tesseract", "spa.traineddata copiado correctamente.");
            }
        } catch (Exception e) {
            Log.e("Tesseract", "Error copiando spa.traineddata: " + e.getMessage(), e);
        }
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
            Scalar lowerYellow = new Scalar(15, 120, 100);
            Scalar upperYellow = new Scalar(45, 255, 255);

            // Crear una máscara para los píxeles amarillos
            Mat yellowMask = new Mat();
            Core.inRange(hsvMat, lowerYellow, upperYellow, yellowMask);

            // Detectar los contornos
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(yellowMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Dibujar contornos y filtrar regiones con proporciones específicas
            for (MatOfPoint contour : contours) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) boundingRect.width / boundingRect.height;
                if (aspectRatio > 1 && aspectRatio < 5) {
                    // Recortar la región de la placa
                    Mat plateRegion = new Mat(mat, boundingRect);

                    // Convertir la región recortada a Bitmap
                    Bitmap plateBitmap = Bitmap.createBitmap(plateRegion.cols(), plateRegion.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(plateRegion, plateBitmap);

                    // Devolver el Bitmap recortado
                    return plateBitmap; // Devuelve el recorte de la placa
                }
            }

            // Si no se detecta ninguna placa, devolver null
            Log.e("ProcessImage", "No se detectaron placas.");
            return null;

        } catch (Exception e) {
            Log.e("ProcessImage", "Error procesando la imagen: " + e.getMessage(), e);
            return null;
        }
    }

}
