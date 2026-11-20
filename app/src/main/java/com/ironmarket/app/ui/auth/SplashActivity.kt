package com.ironmarket.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivitySplashBinding
import com.ironmarket.app.ui.common.Navigation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen: muestra la marca mientras restaura la sesión.
 * Si hay una sesión válida abre el panel del rol; si no, el inicio de sesión.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    /** Configura la pantalla de inicio: infla la vista y registra los eventos de los controles. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animación de entrada del logo
        binding.logo.apply { alpha = 0f; scaleX = 0.7f; scaleY = 0.7f }
            .animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start()

        lifecycleScope.launch {
            val user = runCatching { Session.repo.restoreSession() }.getOrNull()
            delay(1200)
            if (user != null) Navigation.openHome(this@SplashActivity, user)
            else startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }
    }
}
