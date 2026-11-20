package com.ironmarket.app.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Autenticación biométrica (huella) con AndroidX Biometric.
 * La huella se valida en el hardware del dispositivo: la app solo recibe "éxito" o "error",
 * nunca el dato biométrico (Ley 1581 de 2012, dato sensible).
 */
object Biometrics {

    private const val AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

    /** Devuelve null si el dispositivo puede usar huella, o un mensaje explicando por qué no. */
    fun unavailableReason(activity: FragmentActivity): String? =
        when (BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No hay huellas registradas en este dispositivo."
            else -> "Este dispositivo no tiene sensor biométrico disponible."
        }

    /** Muestra el diálogo del sistema y ejecuta [onSuccess] si la huella es válida. */
    fun prompt(activity: FragmentActivity, title: String, subtitle: String,
               onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                /** Huella reconocida: continúa con la acción protegida. */
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                /** Error o cancelación del diálogo de huella. */
                override fun onAuthenticationError(code: Int, msg: CharSequence) = onError(msg.toString())
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle(title).setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setNegativeButtonText("Cancelar")
            .build())
    }
}
