package com.example.appmovil.ui.slideshow;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.appmovil.R;
import com.example.appmovil.databinding.FragmentSlideshowBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.googlecode.tesseract.android.TessBaseAPI;

import androidx.camera.view.PreviewView;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageCapture.Metadata;

import android.graphics.BitmapFactory;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import com.google.mlkit.vision.text.TextRecognition;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private PreviewView cameraPreviewView;
    private ImageView plateImageView;
    private TextView recognizedTextView;
    private ImageCapture imageCapture;
    private TessBaseAPI tessBaseAPI;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        cameraPreviewView = root.findViewById(R.id.camera_preview);
        plateImageView = root.findViewById(R.id.plate_image_view);
        recognizedTextView = root.findViewById(R.id.text_recognized);

        // Iniciar la cámara
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));

        binding.captureButton.setOnClickListener(v -> captureImage());

        return root;
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
    }

    private void captureImage() {
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(createTempFile()).build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // Cargar la imagen capturada desde el archivo temporal
                Bitmap capturedImage = BitmapFactory.decodeFile(createTempFile().getAbsolutePath());

                // Procesar la imagen capturada para segmentar la placa
                Bitmap processedPlateImage = processImage(capturedImage);

                if (processedPlateImage != null) {
                    // Mostrar la imagen de la placa procesada en el ImageView
                    plateImageView.setImageBitmap(processedPlateImage);
                    recognizedTextView.setText("Placa segmentada correctamente.");
                } else {
                    recognizedTextView.setText("No se pudo segmentar la placa.");
                    Toast.makeText(getContext(), "Placa no detectada.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getContext(), "Error al capturar la imagen", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private File createTempFile() {
        // Crear un archivo temporal para almacenar la imagen
        File tempFile = new File(getContext().getCacheDir(), "captured_image.jpg");
        try {
            if (tempFile.createNewFile()) {
                return tempFile;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile;
    }

    private Bitmap processImage(Bitmap bitmap) {

        // Convertir Bitmap a Mat
        Mat matOriginal = new Mat();
        Utils.bitmapToMat(bitmap, matOriginal);

        // Rotar la imagen 90 grados en sentido horario
        Mat mat = new Mat();
        Core.rotate(matOriginal, mat, Core.ROTATE_90_CLOCKWISE);

        // Convertir Mat rotada de nuevo a Bitmap
        //Bitmap plateBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(mat, plateBitmap);

        if (mat.empty()) {
            Log.e("ProcessImage", "El Mat está vacío después de la conversión");
            return null;
        }

        Mat hsvMat = new Mat();
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV);

        Scalar lowerYellow = new Scalar(20, 100, 100);
        Scalar upperYellow = new Scalar(45, 255, 255);

        Mat yellowMask = new Mat();
        Core.inRange(hsvMat, lowerYellow, upperYellow, yellowMask);

        //Bitmap plateBitmap = Bitmap.createBitmap(yellowMask.cols(), yellowMask.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(yellowMask, plateBitmap);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(yellowMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Bitmap plateBitmap = null;
        String recognizedText = "";

        for (MatOfPoint contour : contours) {
            Rect boundingRect = Imgproc.boundingRect(contour);
            double aspectRatio = (double) boundingRect.width / boundingRect.height;

            if (aspectRatio > 1 && aspectRatio < 5 && boundingRect.area() > 1000) {
                Mat plateRegion = new Mat(mat, boundingRect);

                // Calcular padding dinámico basado en las dimensiones del rectángulo
                int paddingX = (int) (boundingRect.width * 0.01); // 10% del ancho
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
                Imgproc.cvtColor(plateRegion, gray, Imgproc.COLOR_RGB2GRAY);

                // Ecualizar el histograma
                Imgproc.equalizeHist(gray, gray);
                // Aumentar el contraste utilizando CLAHE

                Mat enhanced = new Mat();
                Imgproc.createCLAHE(2.0, new org.opencv.core.Size(8, 8)).apply(gray, enhanced);
                // Aplicar un filtro MedianBlur para eliminar bordes y detalles innecesarios

                // Aplicar el umbral de Otsu
                Mat binary = new Mat();
                Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

                // Crear una máscara para las áreas negras puras (por debajo del umbral)
                Mat mask = new Mat();
                Core.inRange(gray, new Scalar(0), new Scalar(50), mask); // Áreas negras puras (ajustar el umbral según necesites)

                // Aumentar el contraste de las áreas oscuras por encima del umbral
                Mat enhanced1 = new Mat();
                Imgproc.equalizeHist(gray, enhanced1);

                // Restaurar las áreas negras puras usando la máscara
                enhanced1.setTo(new Scalar(0), mask);

                Mat matThreshold = new Mat();
                Imgproc.threshold(enhanced1, matThreshold, 10, 255, Imgproc.THRESH_BINARY);

                plateBitmap = Bitmap.createBitmap(matThreshold.cols(), matThreshold.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matThreshold, plateBitmap);

                if (plateBitmap != null) {
                    // Llama al reconocimiento de texto usando ML Kit
                    recognizeTextFromImage(plateBitmap);

                    // Devuelve la imagen procesada para mostrarla en el ImageView
                    return plateBitmap;
                }

            }
        }
        return null;
    }

    private void recognizeTextFromImage(Bitmap bitmap) {
        try {
            // Crea un InputImage a partir del Bitmap
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            // Instancia TextRecognizer
            TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            // Procesa la imagen
            // Procesar la imagen
            textRecognizer.process(image)
                    .addOnSuccessListener(text -> {
                        // Manejar el texto reconocido
                        recognizedTextView.setText(text.getText());
                    })
                    .addOnFailureListener(e -> {
                        // Manejar errores
                        Toast.makeText(getContext(), "Error al reconocer texto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("TextRecognition", "Error procesando la imagen", e);
        }
    }

    private void displayRecognizedText(Text visionText) {
        StringBuilder recognizedText = new StringBuilder();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            recognizedText.append(block.getText()).append("\n");
        }
        recognizedTextView.setText(recognizedText.toString().trim());
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
