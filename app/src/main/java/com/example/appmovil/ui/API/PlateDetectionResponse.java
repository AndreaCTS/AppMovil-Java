package com.example.appmovil.ui.API;

import java.util.List;

public class PlateDetectionResponse {
    private String text;
    private List<Prediction> predictions; // Asumiendo que la API devuelve una lista de predicciones, dependiendo de la respuesta real

    public String getText() {
        return text;
    }

    public List<Prediction> getPredictions() {
        return predictions;
    }

    // Clase para las predicciones de placas
    public static class Prediction {
        private String label;
        private float confidence;

        public String getLabel() {
            return label;
        }

        public float getConfidence() {
            return confidence;
        }
    }
}
