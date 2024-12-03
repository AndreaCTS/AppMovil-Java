package com.example.appmovil.ui.vista;
// Importaciones necesarias
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ShareViewModel extends ViewModel {
    private final MutableLiveData<String> recognizedText = new MutableLiveData<>();

    public LiveData<String> getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String text) {
        recognizedText.setValue(text);
    }
    public void clearRecognizedText() {
        recognizedText.setValue(null);
    }

}
