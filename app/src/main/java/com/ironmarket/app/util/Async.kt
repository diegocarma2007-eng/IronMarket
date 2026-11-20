package com.ironmarket.app.util

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Lanza una corrutina atada al ciclo de vida de la pantalla y muestra los errores como Toast.
 * Si se pasa [loading], la vista se deshabilita mientras la operación termina (evita dobles clics).
 */
fun LifecycleOwner.launchSafe(loading: View? = null, block: suspend () -> Unit): Job = lifecycleScope.launch {
    loading?.isEnabled = false
    try {
        block()
    } catch (e: Exception) {
        val ctx = when (this@launchSafe) {
            is Fragment -> this@launchSafe.context
            is android.content.Context -> this@launchSafe
            else -> null
        }
        ctx?.let { Toast.makeText(it, e.message ?: "Ocurrió un error", Toast.LENGTH_LONG).show() }
    } finally {
        loading?.isEnabled = true
    }
}

/** En fragmentos se usa el ciclo de vida de la vista para no actualizar vistas destruidas. */
fun Fragment.launchView(loading: View? = null, block: suspend () -> Unit): Job =
    viewLifecycleOwner.lifecycleScope.launch {
        loading?.isEnabled = false
        try { block() }
        catch (e: Exception) { context?.let { Toast.makeText(it, e.message ?: "Ocurrió un error", Toast.LENGTH_LONG).show() } }
        finally { loading?.isEnabled = true }
    }
