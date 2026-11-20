package com.ironmarket.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ironmarket.app.R
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityRecoverBinding
import com.ironmarket.app.util.isValidEmail
import com.ironmarket.app.util.launchSafe

/**
 * Recuperación de contraseña con Firebase Authentication (sendPasswordResetEmail).
 * Por seguridad el mensaje es el mismo exista o no el correo.
 */
class RecoverPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecoverBinding

    /** Configura la recuperación de contraseña: infla la vista y registra los eventos de los controles. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.backToLogin.setOnClickListener { finish() }
        binding.sendButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            if (!email.isValidEmail()) { binding.emailLayout.error = getString(R.string.error_email); return@setOnClickListener }
            binding.emailLayout.error = null
            launchSafe(binding.sendButton) {
                runCatching { Session.repo.sendPasswordReset(email) }   // no se revela si el correo existe
                binding.formGroup.visibility = View.GONE
                binding.doneBody.text = "Si $email está registrado, recibirás un enlace para crear una nueva contraseña."
                binding.doneCard.visibility = View.VISIBLE
            }
        }
    }
}
