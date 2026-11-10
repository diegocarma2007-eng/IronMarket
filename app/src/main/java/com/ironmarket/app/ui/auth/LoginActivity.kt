package com.ironmarket.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.data.User
import com.ironmarket.app.databinding.ActivityLoginBinding
import com.ironmarket.app.ui.admin.AdminActivity
import com.ironmarket.app.ui.buyer.BuyerActivity
import com.ironmarket.app.ui.seller.SellerActivity
import com.ironmarket.app.util.isValidEmail

/**
 * Inicio de sesión (RF de Autenticación):
 * correo y contraseña, ingreso con huella (BiometricPrompt) y redirección según el rol.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener { loginWithPassword() }
        binding.biometricButton.setOnClickListener { loginWithBiometrics() }
        binding.forgotButton.setOnClickListener {
            startActivity(Intent(this, RecoverPasswordActivity::class.java))
        }
        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Cuentas de prueba: llenan el formulario para la demostración
        binding.chipBuyer.setOnClickListener { fillDemo("comprador@ironmarket.co") }
        binding.chipSeller.setOnClickListener { fillDemo("vendedor@ironmarket.co") }
        binding.chipAdmin.setOnClickListener { fillDemo("admin@ironmarket.co") }

        Session.lastEmail(this)?.let { binding.emailInput.setText(it) }
    }

    private fun fillDemo(email: String) {
        binding.emailInput.setText(email)
        binding.passwordInput.setText("123456")
    }

    private fun loginWithPassword() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        binding.emailLayout.error = null
        binding.passwordLayout.error = null

        // Validaciones del formulario
        if (!email.isValidEmail()) { binding.emailLayout.error = getString(R.string.error_email); return }
        if (password.length < 6) { binding.passwordLayout.error = getString(R.string.error_password); return }

        val user = DataRepository.findUserByEmail(email)
        if (user == null || user.password != password) {
            binding.passwordLayout.error = getString(R.string.error_credentials)
            return
        }
        if (!user.active) {
            Toast.makeText(this, "Tu cuenta está suspendida. Contacta a soporte.", Toast.LENGTH_LONG).show()
            return
        }
        openHome(user)
    }

    private fun loginWithBiometrics() {
        val email = Session.lastEmail(this)
        val user = email?.let { DataRepository.findUserByEmail(it) }
        if (user == null || !Session.isBiometricEnabled(this)) {
            Toast.makeText(this, "Primero inicia sesión con tu correo para activar la huella.", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Verificar que el dispositivo tenga sensor y huellas registradas
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK
        when (BiometricManager.from(this).canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No hay huellas registradas en este dispositivo.", Toast.LENGTH_LONG).show(); return
            }
            else -> {
                Toast.makeText(this, "Este dispositivo no tiene sensor biométrico disponible.", Toast.LENGTH_LONG).show(); return
            }
        }

        // 2. Mostrar el diálogo del sistema. La huella nunca sale del dispositivo.
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    openHome(user)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(this@LoginActivity, errString, Toast.LENGTH_SHORT).show()
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.bio_prompt_title))
            .setSubtitle(user.email)
            .setDescription(getString(R.string.bio_prompt_subtitle))
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText(getString(R.string.cancel))
            .build()
        prompt.authenticate(info)
    }

    /** Guarda la sesión y abre el panel del rol correspondiente. */
    private fun openHome(user: User) {
        Session.login(this, user)
        val target = when (user.role) {
            Role.COMPRADOR -> BuyerActivity::class.java
            Role.VENDEDOR -> SellerActivity::class.java
            Role.ADMIN -> AdminActivity::class.java
        }
        startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
}
