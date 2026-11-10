package com.ironmarket.app.ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ironmarket.app.data.Session

/**
 * Actividad base para las pantallas de cada rol.
 * Aplica el tema del rol (verde, naranja o azul) antes de crear las vistas.
 */
abstract class RoleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Session.themeFor(Session.role))
        super.onCreate(savedInstanceState)
    }
}
