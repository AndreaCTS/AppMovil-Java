package com.example.appmovil.ui.vista;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appmovil.MainActivity;
import com.example.appmovil.R;

public class SplashScreen extends AppCompatActivity implements Runnable {


    Thread h1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        ImageView imagen = (ImageView)findViewById(R.id.parqueadero);
        imagen.setBackgroundResource(R.drawable.secuencia_animacion);

        AnimationDrawable ejecutarAnimacion = (AnimationDrawable)imagen.getBackground();
        ejecutarAnimacion.start();

        //***********************
        h1= new Thread(this);
        h1.start();
        //***********************

    }

    @Override
    public void run() {
        try {
            Thread.sleep(1500);
            Intent pasarPantalla = new Intent(getApplicationContext(), MainActivity.class );
            startActivity(pasarPantalla);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            finish();
        }
    }
}