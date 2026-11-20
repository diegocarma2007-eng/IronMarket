package com.ironmarket.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ironmarket.app.R
import com.ironmarket.app.data.DemoData
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityLoginBinding
import com.ironmarket.app.ui.common.Navigation
import com.ironmarket.app.util.Biometrics
import com.ironmarket.app.util.isValidEmail
import com.ironmarket.app.util.launchSafe

/**
 * Inicio de sesión (RF de Autenticación):
 * correo y contraseña contra Firebase Authentication (o SQLite en modo local),
 * ingreso con huella usando credenciales cifradas, y redirección según el rol.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    /** Configura el inicio de sesión: infla la vista y registra los eventos de los controles. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener { loginWithPassword() }
        binding.biometricButton.setOnClickListener { loginWithBiometrics() }
        binding.forgotButton.setOnClickListener { startActivity(Intent(this, RecoverPasswordActivity::class.java)) }
        binding.registerButton.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }

        // Cuentas de prueba: llenan el formulario para la demostración
        binding.chipBuyer.setOnClickListener { fillDemo("comprador@ironmarket.co") }
        binding.chipSeller.setOnClickListener { fillDemo("vendedor@ironmarket.co") }
        binding.chipAdmin.setOnClickListener { fillDemo("admin@ironmarket.co") }

        Session.lastEmail(this)?.let { binding.emailInput.setText(it) }
    }

    /** Llena el formulario con una cuenta de prueba. */
    private fun fillDemo(email: String) {
        binding.emailInput.setText(email)
        binding.passwordInput.setText(DemoData.DEMO_PASSWORD)
    }

    /** Valida el formulario y autentica con correo y contraseña. */
    private fun loginWithPassword() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        if (!email.isValidEmail()) { binding.emailLayout.error = getString(R.string.error_email); return }
        if (password.length < 6) { binding.passwordLayout.error = getString(R.string.error_password); return }

        launchSafe(binding.loginButton) {
            val user = Session.repo.login(email, password)
            Session.saveCredentials(this, email, password)   // contraseña cifrada para el ingreso con huella
            Navigation.openHome(this, user)
        }
    }

    /** Pide la huella y, si es válida, inicia sesión con las credenciales guardadas cifradas. */
    private fun loginWithBiometrics() {
        val credentials = Session.storedCredentials(this)
        if (credentials == null || !Session.isBiometricEnabled(this)) {
            Toast.makeText(this, "Primero inicia sesión con tu correo para activar la huella.", Toast.LENGTH_LONG).show()
            return
        }
        Biometrics.unavailableReason(this)?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show(); return }
        Biometrics.prompt(this, getString(R.string.bio_prompt_title), credentials.first, onSuccess = {
            launchSafe(binding.biometricButton) {
                Navigation.openHome(this, Session.repo.login(credentials.first, credentials.second))
            }
        })
    }
}
