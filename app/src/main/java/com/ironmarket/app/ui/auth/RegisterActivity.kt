package com.ironmarket.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ironmarket.app.R
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityRegisterBinding
import com.ironmarket.app.ui.common.Navigation
import com.ironmarket.app.util.isValidEmail
import com.ironmarket.app.util.launchSafe

/** Registro con selección de rol y autorización de tratamiento de datos (Ley 1581 de 2012). */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    /** Configura el registro de cuentas: infla la vista y registra los eventos de los controles. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.roleToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) binding.sellerNote.visibility = if (checkedId == R.id.roleSeller) View.VISIBLE else View.GONE
        }
        binding.registerButton.setOnClickListener { register() }
    }

    /** Valida todos los campos y crea la cuenta en la fuente de datos. */
    private fun register() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        listOf(binding.nameLayout, binding.emailLayout, binding.phoneLayout, binding.passwordLayout).forEach { it.error = null }

        var valid = true
        if (name.length < 3) { binding.nameLayout.error = getString(R.string.error_required); valid = false }
        if (!email.isValidEmail()) { binding.emailLayout.error = getString(R.string.error_email); valid = false }
        if (phone.length != 10) { binding.phoneLayout.error = "Escribe 10 dígitos"; valid = false }
        if (password.length < 6) { binding.passwordLayout.error = getString(R.string.error_password); valid = false }
        if (!binding.consentData.isChecked) { Toast.makeText(this, R.string.error_consent, Toast.LENGTH_SHORT).show(); valid = false }
        if (!valid) return

        val role = if (binding.roleToggle.checkedButtonId == R.id.roleSeller) Role.VENDEDOR else Role.COMPRADOR
        launchSafe(binding.registerButton) {
            val user = Session.repo.register(name, email, phone, password, role)
            Session.setBiometricEnabled(this, binding.consentBio.isChecked)
            Session.saveCredentials(this, email, password)
            Toast.makeText(this, "¡Cuenta creada! Bienvenido, ${user.name}", Toast.LENGTH_SHORT).show()
            Navigation.openHome(this, user)
        }
    }
}
