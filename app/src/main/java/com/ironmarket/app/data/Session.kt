package com.ironmarket.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.firebase.FirebaseApp
import com.ironmarket.app.R

/**
 * Punto de acceso global: elige la fuente de datos y guarda la sesión del usuario actual.
 *
 * - Si Firebase está configurado (existe google-services.json) usa [FirebaseRepository].
 * - Si no, usa [SqliteRepository] (modo local).
 *
 * Las credenciales para el ingreso con huella se guardan cifradas con EncryptedSharedPreferences,
 * cuya llave maestra vive en el Android Keystore del dispositivo.
 */
object Session {

    lateinit var repo: Repository
        private set

    /** Usuario con sesión iniciada. */
    var currentUser: User? = null

    val role: Role get() = currentUser?.role ?: Role.COMPRADOR

    /** Inicializa la fuente de datos. Se llama una sola vez desde [com.ironmarket.app.IronMarketApp]. */
    fun init(context: Context) {
        if (::repo.isInitialized) return
        repo = if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseRepository(context.applicationContext)
               else SqliteRepository(context.applicationContext)
    }

    /** Permite inyectar un repositorio (pruebas automáticas). */
    fun useRepository(repository: Repository) { repo = repository }

    /** Tema según el rol: cambia el color de acento de toda la interfaz. */
    fun themeFor(role: Role) = when (role) {
        Role.COMPRADOR -> R.style.Theme_IronMarket_Buyer
        Role.VENDEDOR -> R.style.Theme_IronMarket_Seller
        Role.ADMIN -> R.style.Theme_IronMarket_Admin
    }

    /** Cierra la sesión actual. */
    fun logout() {
        repo.logout()
        currentUser = null
    }

    // ---------------- Preferencias y credenciales cifradas ----------------

    /** Preferencias simples de la app (último correo). */
    private fun prefs(context: Context) = context.getSharedPreferences("ironmarket_session", Context.MODE_PRIVATE)

    /** Preferencias cifradas con Android Keystore para las credenciales de la huella. */
    private fun securePrefs(context: Context) = EncryptedSharedPreferences.create(
        "ironmarket_secure",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),   // llave maestra en el Android Keystore
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Último correo con el que se inició sesión. */
    fun lastEmail(context: Context): String? = prefs(context).getString("last_email", null)

    /** Indica si el usuario activó el ingreso con huella. */
    fun isBiometricEnabled(context: Context) = prefs(context).getBoolean("biometric_enabled", true)

    /** Activa o desactiva el ingreso con huella. */
    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("biometric_enabled", enabled).apply()
        if (!enabled) runCatching { securePrefs(context).edit().clear().apply() }
    }

    /** Guarda el correo (en claro) y la contraseña (cifrada) tras un inicio de sesión exitoso. */
    fun saveCredentials(context: Context, email: String, password: String) {
        prefs(context).edit().putString("last_email", email).apply()
        if (isBiometricEnabled(context)) runCatching {
            securePrefs(context).edit().putString("email", email).putString("password", password).apply()
        }
    }

    /** Devuelve las credenciales guardadas para el ingreso con huella, o null. */
    fun storedCredentials(context: Context): Pair<String, String>? = runCatching {
        val p = securePrefs(context)
        val e = p.getString("email", null); val pw = p.getString("password", null)
        if (e != null && pw != null) e to pw else null
    }.getOrNull()
}
