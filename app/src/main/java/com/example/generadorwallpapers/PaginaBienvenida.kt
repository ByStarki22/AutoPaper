package com.example.generadorwallpapers
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PaginaBienvenida : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pagina_bienvenida)

        val imagenBienvenida = findViewById<ImageView>(R.id.imagenBienvenida)

        // Cargar y aplicar la animación
        val animation = AnimationUtils.loadAnimation(this, R.anim.translate_bounce)
        imagenBienvenida.startAnimation(animation)

        // Duración del splash screen en milisegundos
        val splashScreenDuration = 3000L

        Handler(Looper.getMainLooper()).postDelayed({
            // Navegar a la actividad principal
            startActivity(Intent(this, MainActivity::class.java))
            // Cerrar la actividad de la ventana de bienvenida
            finish()
        }, splashScreenDuration)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
