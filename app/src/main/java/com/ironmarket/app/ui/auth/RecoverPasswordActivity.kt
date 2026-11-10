package com.ironmarket.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ironmarket.app.R
import com.ironmarket.app.databinding.ActivityRecoverBinding
import com.ironmarket.app.util.isValidEmail

/**
 * Recuperación de contraseña. Por seguridad no se revela si el correo existe
 * (RF de Autenticación, documento técnico).
 */
class RecoverPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecoverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.backToLogin.setOnClickListener { finish() }
        binding.sendButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            if (!email.isValidEmail()) {
                binding.emailLayout.error = getString(R.string.error_email)
                return@setOnClickListener
            }
            binding.emailLayout.error = null
            binding.formGroup.visibility = View.GONE
            binding.doneBody.text = "Enviamos un enlace a $email. " + getString(R.string.recover_done_body)
            binding.doneCard.visibility = View.VISIBLE
        }
    }
}
