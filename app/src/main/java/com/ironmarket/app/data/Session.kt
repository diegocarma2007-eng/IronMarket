package com.ironmarket.app.data

import android.content.Context
import com.ironmarket.app.R

/** Sesión del usuario actual y preferencias guardadas en el dispositivo. */
object Session {
    private const val PREFS = "ironmarket_session"
    private const val KEY_LAST_EMAIL = "last_email"
    private const val KEY_BIOMETRIC = "biometric_enabled"

    var currentUser: User? = null

    val role: Role get() = currentUser?.role ?: Role.COMPRADOR

    /** Tema según el rol: cambia el color de acento de toda la interfaz. */
    fun themeFor(role: Role) = when (role) {
        Role.COMPRADOR -> R.style.Theme_IronMarket_Buyer
        Role.VENDEDOR -> R.style.Theme_IronMarket_Seller
        Role.ADMIN -> R.style.Theme_IronMarket_Admin
    }

    fun login(context: Context, user: User) {
        currentUser = user
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_EMAIL, user.email).apply()
    }

    fun logout() {
        currentUser = null
        DataRepository.cart.clear()
    }

    fun lastEmail(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_EMAIL, null)

    fun isBiometricEnabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_BIOMETRIC, true)

    fun setBiometricEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
}
