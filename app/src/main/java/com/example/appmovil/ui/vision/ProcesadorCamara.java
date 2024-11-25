package com.example.appmovil.ui.vision;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Scalar;
import org.opencv.core.Rect;
import org.opencv.core.CvType;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ProcesadorCamara {

    private String esp32Url;
    private ImageView imageView;

    public ProcesadorCamara(String esp32Url, ImageView imageView) {
        this.esp32Url = esp32Url;
        this.imageView = imageView;
    }

    public void fetchAndProcessImage() {
        new DownloadImageTask().execute(esp32Url);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    Log.d("ImageDownload", "Imagen descargada con éxito");
                } else {
                    Log.e("ImageDownload", "Error al descargar la imagen: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e("ImageDownload", "Error: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("MANDAAA"+bitmap);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            System.out.println("ENTRAAAA3");
            if (bitmap != null) {
                Mat processedMat = processImage(bitmap);
                System.out.println("ENTRAAAA2");
                Bitmap processedBitmap = Bitmap.createBitmap(processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(processedMat, processedBitmap);
                imageView.setImageBitmap(processedBitmap);
            }
        }
    }

    // Función para procesar la imagen y detectar placas amarillas
    private Mat processImage(Bitmap bitmap) {
        System.out.println("ENTRAAAA");
        // Convertir el Bitmap a Mat (estructura de OpenCV)
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        // Convertir la imagen a espacio de color HSV
        Mat hsvMat = new Mat();
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV);

        // Definir el rango de color amarillo en HSV
        Scalar lowerYellow = new Scalar(20, 100, 100);  // Valor mínimo para amarillo
        Scalar upperYellow = new Scalar(30, 255, 255);  // Valor máximo para amarillo

        // Crear una máscara para los píxeles amarillos
        Mat yellowMask = new Mat();
        Core.inRange(hsvMat, lowerYellow, upperYellow, yellowMask);

        // Aplicar la máscara a la imagen original para extraer la región amarilla
        Mat yellowRegion = new Mat();
        Core.bitwise_and(mat, mat, yellowRegion, yellowMask);

        // Convertir la imagen a escala de grises para la detección de contornos
        Mat grayMat = new Mat();
        Imgproc.cvtColor(yellowRegion, grayMat, Imgproc.COLOR_BGR2GRAY);

        // Aplicar un umbral para obtener una imagen binaria
        Mat thresholdMat = new Mat();
        Imgproc.threshold(grayMat, thresholdMat, 120, 255, Imgproc.THRESH_BINARY);

        // Detectar los contornos de las placas
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholdMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Filtrar los contornos que podrían ser placas (por ejemplo, un área rectangular)
        for (MatOfPoint contour : contours) {
            Rect boundingRect = Imgproc.boundingRect(contour);
            double aspectRatio = (double) boundingRect.width / boundingRect.height;

            // Comprobar si el aspecto corresponde a una placa (relación de aspecto típica entre 2 y 4)
            if (aspectRatio > 2 && aspectRatio < 4) {
                // Dibujar un rectángulo alrededor de la posible placa
                Imgproc.rectangle(mat, boundingRect.tl(), boundingRect.br(), new Scalar(0, 255, 0), 2);

                // Aquí podrías agregar el código para extraer la placa utilizando OCR (Tesseract)
                Mat plateMat = mat.submat(boundingRect);
                String plateText = extractPlateText(plateMat);
                Log.d("Detected Plate", plateText);
            }
        }

        return mat;
    }

    // Método de ejemplo para usar OCR para extraer el texto de la placa
    private String extractPlateText(Mat plateMat) {
        // Aquí es donde usarías un OCR como Tesseract para leer el texto de la placa
        // Ejemplo de uso con Tesseract:
        // TessBaseAPI tessBaseAPI = new TessBaseAPI();
        // tessBaseAPI.init("/path/to/tessdata", "eng");
        // tessBaseAPI.setImage(plateMat);
        // String plateText = tessBaseAPI.getUTF8Text();
        // return plateText;
        return "ABC123"; // Placeholder
    }
}
