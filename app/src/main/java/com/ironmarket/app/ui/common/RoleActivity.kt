package com.ironmarket.app.ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ironmarket.app.data.Session
import com.ironmarket.app.ui.auth.SplashActivity

/**
 * Actividad base para las pantallas posteriores al inicio de sesión.
 * - Aplica el tema del rol (verde, naranja o azul) antes de crear las vistas.
 * - Si Android cerró el proceso y se perdió la sesión en memoria, vuelve al splash para restaurarla.
 */
abstract class RoleActivity : AppCompatActivity() {

    /** true si la pantalla puede continuar (hay usuario en sesión). */
    protected var hasSession = true
        private set

    /** Si no hay sesión activa, redirige al splash antes de crear la pantalla. */
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Session.themeFor(Session.role))
        super.onCreate(savedInstanceState)
        if (Session.currentUser == null) {
            hasSession = false
            startActivity(android.content.Intent(this, SplashActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }
}
