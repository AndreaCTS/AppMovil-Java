package com.example.appmovil;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;

import com.example.appmovil.ui.API.ApiService;
import com.example.appmovil.ui.API.PlateDetectionResponse;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appmovil.databinding.ActivityMainBinding;

import org.opencv.android.OpenCVLoader;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private static final String API_KEY = "ltYcbmlWqLN7iFksT3bn"; // La clave de la API de Roboflow


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("MainActivity", "onCreate llamado"); // Asegúrate de que onCreate se ejecuta

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if(OpenCVLoader.initDebug()) {
            Log.d("OPEN2023CV", "SUCCESS");
        } else {
            Log.d("OPEN2023CV", "ERROR");
        }

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Ruta de la imagen que deseas enviar
        File file = new File("/sdcard/DCIM/Camera/20241125_212718.jpg"); // Cambia esto a la ruta de tu imagen
        Log.d("MainActivity", "Archivo de imagen preparado: " + file.getAbsolutePath());

        // Crear el cuerpo de la solicitud con la imagen y la clave de la API
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        RequestBody apiKey = RequestBody.create(MediaType.parse("text/plain"), API_KEY);

        // Configurar Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://detect.roboflow.com")
                .addConverterFactory(GsonConverterFactory.create()) // Usar el convertidor Gson
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<PlateDetectionResponse> call = apiService.inferPlate(body, apiKey);

        // Realizar la solicitud
        Log.d("MainActivity", "Llamando a la API...");
        call.enqueue(new Callback<PlateDetectionResponse>() {
            @Override
            public void onResponse(Call<PlateDetectionResponse> call, Response<PlateDetectionResponse> response) {
                Log.d("PlateDetection", "onResponse llamado");

                if (response.isSuccessful()) {
                    PlateDetectionResponse result = response.body();
                    if (result != null) {
                        // Procesar el resultado (puedes acceder a los detalles de la placa, como texto, coordenadas, etc.)
                        String plateText = result.getText();  // O como sea que esté estructurada la respuesta
                        Log.d("PlateDetection", "Placa detectada: " + plateText);
                    }
                } else {
                    // Manejar el error
                    Log.e("PlateDetection", "Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PlateDetectionResponse> call, Throwable t) {
                Log.e("PlateDetection", "Fallo en la solicitud: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}