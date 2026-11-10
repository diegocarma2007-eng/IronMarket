package com.ironmarket.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.ironmarket.app.databinding.ActivitySplashBinding

/** Splash screen: muestra la marca 1,5 segundos y abre el inicio de sesión. */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animación de entrada del logo
        binding.logo.apply { alpha = 0f; scaleX = 0.7f; scaleY = 0.7f }
            .animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 1500)
    }
}
