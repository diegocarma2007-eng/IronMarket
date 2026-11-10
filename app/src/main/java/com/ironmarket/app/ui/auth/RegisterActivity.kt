package com.ironmarket.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityRegisterBinding
import com.ironmarket.app.ui.buyer.BuyerActivity
import com.ironmarket.app.ui.seller.SellerActivity
import com.ironmarket.app.util.isValidEmail

/** Registro con selección de rol y autorización de tratamiento de datos. */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.roleToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) binding.sellerNote.visibility =
                if (checkedId == R.id.roleSeller) View.VISIBLE else View.GONE
        }
        binding.registerButton.setOnClickListener { register() }
    }

    private fun register() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        listOf(binding.nameLayout, binding.emailLayout, binding.phoneLayout, binding.passwordLayout).forEach { it.error = null }

        var valid = true
        if (name.length < 3) { binding.nameLayout.error = getString(R.string.error_required); valid = false }
        if (!email.isValidEmail()) { binding.emailLayout.error = getString(R.string.error_email); valid = false }
        else if (DataRepository.findUserByEmail(email) != null) { binding.emailLayout.error = "Este correo ya está registrado"; valid = false }
        if (phone.length != 10) { binding.phoneLayout.error = "Escribe 10 dígitos"; valid = false }
        if (password.length < 6) { binding.passwordLayout.error = getString(R.string.error_password); valid = false }
        if (!binding.consentData.isChecked) { Toast.makeText(this, R.string.error_consent, Toast.LENGTH_SHORT).show(); valid = false }
        if (!valid) return

        val role = if (binding.roleToggle.checkedButtonId == R.id.roleSeller) Role.VENDEDOR else Role.COMPRADOR
        val user = DataRepository.createUser(name, email, phone, role)
        Session.setBiometricEnabled(this, binding.consentBio.isChecked)
        Session.login(this, user)
        Toast.makeText(this, "¡Cuenta creada! Bienvenido, ${user.name}", Toast.LENGTH_SHORT).show()

        val target = if (role == Role.VENDEDOR) SellerActivity::class.java else BuyerActivity::class.java
        startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
}
