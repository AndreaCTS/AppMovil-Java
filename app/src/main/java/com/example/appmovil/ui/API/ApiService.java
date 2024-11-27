package com.example.appmovil.ui.API;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import okhttp3.RequestBody;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("/v1/detect")
    Call<PlateDetectionResponse> inferPlate(
            @Part MultipartBody.Part image,  // Imagen como parte del formulario
            @Part("apiKey") RequestBody apiKey  // API Key como otro campo del formulario
    );
}
