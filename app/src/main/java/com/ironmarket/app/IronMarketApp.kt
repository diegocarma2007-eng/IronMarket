package com.ironmarket.app

import android.app.Application
import com.ironmarket.app.data.Session

/** Clase Application: inicializa la fuente de datos al arrancar la app. */
class IronMarketApp : Application() {
    /** Punto de entrada del proceso: elige la fuente de datos (Firebase o SQLite). */
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
    }
}
