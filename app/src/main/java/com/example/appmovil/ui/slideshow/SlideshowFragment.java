package com.example.appmovil.ui.slideshow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.appmovil.R;
import com.example.appmovil.databinding.FragmentSlideshowBinding;
import com.example.appmovil.ui.wifi.WifiHandler;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class SlideshowFragment extends Fragment {
    private static final String ESP32_URL = "192.168.4.1/descargarImagen"; // IP del ESP32 sin "http://"
    private FragmentSlideshowBinding binding;
    private ImageView imageView;
    private TessBaseAPI tessBaseAPI;
    private WifiHandler wifiHandler;

    private android.os.Handler handler; // Handler para la tarea periódica
    private Runnable refreshTask; // Tarea de actualización periódica


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        imageView = binding.imageView;

        // Inicializa WifiHandler para interactuar con el ESP32
        wifiHandler = new WifiHandler(ESP32_URL);

        // Copia los datos de Tesseract al sistema de archivos del dispositivo
        prepareTesseractData();

        // Inicializar Tesseract
        String dataPath = getActivity().getFilesDir() + "/tesseract/";
        tessBaseAPI = new TessBaseAPI();
        if (!tessBaseAPI.init(dataPath, "spa")) {
            Log.e("Tesseract", "No se pudo inicializar Tesseract");
        }

        // Configurar la tarea de actualización periódica
        handler = new android.os.Handler();
        refreshTask = new Runnable() {
            @Override
            public void run() {
                wifiHandler.fetchImage(bitmap -> {

                    if (bitmap != null) {
                        Log.d("ImageView", "El Bitmap tiene dimensiones: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        saveBitmapToFile(bitmap, "descargada.jpg");
                        processAndDisplayImage(bitmap);

                    } else {
                        Log.e("SlideshowFragment", "No se pudo descargar la imagen desde el ESP32.");
                    }
                });

                // Reprogramar la tarea para que se ejecute cada 5 segundos
                handler.postDelayed(this, 5000); // Intervalo de actualización
            }
        };
        handler.post(refreshTask); // Inicia la tarea periódica

        return root;
    }
    private void saveBitmapToFile(Bitmap bitmap, String fileName) {
        try {
            // Obtén la ruta del almacenamiento interno
            File directory = getActivity().getExternalFilesDir(null);
            if (directory == null) {
                Log.e("SaveBitmap", "El directorio de almacenamiento es nulo.");
                return;
            }

            // Crea el archivo
            File file = new File(directory, fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            // Escribe el bitmap en el archivo en formato JPEG
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            Log.d("SaveBitmap", "Imagen guardada en: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e("SaveBitmap", "Error al guardar el bitmap: " + e.getMessage(), e);
        }
    }

    private void processAndDisplayImage(Bitmap bitmap) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            if(bitmap != null) {
                Bitmap[] bitmaps = processImage(bitmap);
                //if (bitmaps != null && bitmaps[1] != null) {
                //    Bitmap processedBitmap = bitmaps[0]; // Imagen procesada con rectángulos
                //    Bitmap plateBitmap = bitmaps[1];    // Recorte de la placa

                    getActivity().runOnUiThread(() -> {
                        imageView.setImageBitmap(bitmap); // Mostrar imagen completa procesada
                        imageView.requestLayout();
                        //ImageView plateImageView = binding.plateImageView; // Otro ImageView para la placa
                        //plateImageView.setImageBitmap(plateBitmap); // Mostrar la placa recortada
                        //plateImageView.requestLayout();

                    });

                }
            //}
            else{
                System.out.println("es vacio ");
            }
        });
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


    private Bitmap[] processImage(Bitmap bitmap) {
        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);

            if (mat.empty()) {
                Log.e("ProcessImage", "El Mat está vacío después de la conversión");
                return null;
            }

            Mat hsvMat = new Mat();
            Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV);

            // Definir un rango de amarillo para las placas
            Scalar lowerYellow = new Scalar(25, 100, 100);
            Scalar upperYellow = new Scalar(30, 255, 255);

            Mat yellowMask = new Mat();
            Core.inRange(hsvMat, lowerYellow, upperYellow, yellowMask);

            // Encontrar contornos
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(yellowMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Bitmap plateBitmap = null;
            String recognizedText = "";

            // Filtrar los contornos para encontrar los más relevantes (placa)
            for (MatOfPoint contour : contours) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) boundingRect.width / boundingRect.height;
                if (aspectRatio > 1 && aspectRatio < 5 && boundingRect.area() > 1000) {
                    // Extraer la región de la placa
                    Mat plateRegion = new Mat(mat, boundingRect);

                    //plateBitmap = Bitmap.createBitmap(plateRegion.cols(), plateRegion.rows(), Bitmap.Config.ARGB_8888);
                    //Utils.matToBitmap(plateRegion, plateBitmap);

                    // Calcular padding dinámico basado en las dimensiones del rectángulo
                    int paddingX = (int) (boundingRect.width * 0.05); // 10% del ancho
                    int paddingY = (int) (boundingRect.height * 0.1); // 10% de la altura

                    // Ajustar los límites para evitar recortar fuera de la imagen
                    int x = Math.max(boundingRect.x + paddingX, 0);
                    int y = Math.max(boundingRect.y + paddingY, 0);
                    int width = Math.min(boundingRect.width - 2 * paddingX, mat.cols() - x);
                    int height = Math.min(boundingRect.height - 2 * paddingY, mat.rows() - y);

                    if (width <= 0 || height <= 0) continue; // Ignorar si el recorte no es válido

                    Rect croppedRect = new Rect(x, y, width, height);
                    Mat croppedPlateRegion = new Mat(mat, croppedRect);

                    // Convertir a escala de grises
                    Mat gray = new Mat();
                    Imgproc.cvtColor(croppedPlateRegion, gray, Imgproc.COLOR_RGB2GRAY);

                    // Mejorar el contraste y la visibilidad de las letras
                    Imgproc.equalizeHist(gray, gray);

                    // Aumentar el contraste usando CLAHE
                    Mat enhanced = new Mat();
                    Imgproc.createCLAHE(2.0, new org.opencv.core.Size(8, 8)).apply(gray, enhanced);

                    Mat binary = new Mat();
                    Imgproc.threshold(enhanced, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
                    //Mat binary = new Mat();
                    //Imgproc.adaptiveThreshold(gray, binary, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 11, 2);

                    // Crear una máscara para las áreas negras puras (por debajo del umbral)
                    Mat mask = new Mat();
                    Core.inRange(enhanced, new Scalar(0), new Scalar(50), mask); // Áreas negras puras (ajustar el umbral según necesites)

                    Mat inverseMask = new Mat();
                    Core.bitwise_not(mask, inverseMask); // Invertir la máscara

                    // Asignar las áreas no negras a blanco
                    Mat result = new Mat(enhanced.size(), CvType.CV_8UC1, new Scalar(0)); // Crear una imagen negra
                    result.setTo(new Scalar(255), inverseMask); // Asignar blanco a las áreas no negras

                    // Convertir a Bitmap para mostrar o guardar
                    plateBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(result, plateBitmap);

                    // Reconocer texto en la imagen de la placa
                    //tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
                    // Reconocer texto en la imagen de la placa
                    //tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE); // Modo de OCR para una sola línea
                    tessBaseAPI.setImage(plateBitmap);
                    recognizedText = tessBaseAPI.getUTF8Text().replaceAll("[^A-Z0-9]", ""); // Filtrar solo caracteres válidos
                    Log.d("Tesseract", "Texto reconocido: " + recognizedText);

                    break; // Solo procesar la primera placa detectada
                }
            }

            // Convertir la imagen completa a Bitmap (sin ningún borde o rectángulo marcado)
            Bitmap processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, processedBitmap);

            Log.d("ProcessImage", "Texto final reconocido: " + recognizedText);

            return new Bitmap[]{processedBitmap, plateBitmap}; // Devuelve ambas imágenes: la completa y la de la placa
        } catch (Exception e) {
            Log.e("ProcessImage", "Error procesando la imagen: " + e.getMessage(), e);
            return null;
        }
    }

}