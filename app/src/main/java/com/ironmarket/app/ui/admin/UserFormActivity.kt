package com.ironmarket.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.ironmarket.app.R
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.data.User
import com.ironmarket.app.databinding.ActivityUserFormBinding
import com.ironmarket.app.ui.common.RoleActivity
import com.ironmarket.app.util.isValidEmail
import com.ironmarket.app.util.launchSafe

/** Crear (C) y editar (U) usuarios desde la administración. */
class UserFormActivity : RoleActivity() {

    companion object { const val EXTRA_ID = "user_id" }

    private lateinit var binding: ActivityUserFormBinding
    private var editing: User? = null

    /** Configura el formulario de usuario: infla la vista y registra los eventos de los controles. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasSession) return
        binding = ActivityUserFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.closeButton.setOnClickListener { finish() }
        binding.header.formTitle.text = getString(R.string.new_user)
        binding.saveButton.setOnClickListener { save() }

        intent.getStringExtra(EXTRA_ID)?.let { id ->
            binding.header.formTitle.text = getString(R.string.edit_user)
            binding.passwordLayout.visibility = View.GONE   // la contraseña solo se define al crear
            binding.emailInput.isEnabled = false            // el correo es el identificador de acceso
            launchSafe {
                val u = Session.repo.getUser(id) ?: return@launchSafe
                editing = u
                binding.nameInput.setText(u.name)
                binding.emailInput.setText(u.email)
                binding.phoneInput.setText(u.phone)
                binding.roleToggle.check(when (u.role) {
                    Role.COMPRADOR -> R.id.roleBuyer
                    Role.VENDEDOR -> R.id.roleSeller
                    Role.ADMIN -> R.id.roleAdmin
                })
                binding.activeSwitch.isChecked = u.active
                binding.verifiedSwitch.isChecked = u.verified
            }
        }
    }

    /** Valida el formulario y crea o actualiza el usuario. */
    private fun save() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        binding.nameLayout.error = null; binding.emailLayout.error = null; binding.passwordLayout.error = null

        var ok = true
        if (name.length < 3) { binding.nameLayout.error = getString(R.string.error_required); ok = false }
        if (!email.isValidEmail()) { binding.emailLayout.error = getString(R.string.error_email); ok = false }
        if (editing == null && password.length < 6) { binding.passwordLayout.error = getString(R.string.error_password); ok = false }
        if (!ok) return

        val role = when (binding.roleToggle.checkedButtonId) {
            R.id.roleSeller -> Role.VENDEDOR
            R.id.roleAdmin -> Role.ADMIN
            else -> Role.COMPRADOR
        }
        launchSafe(binding.saveButton) {
            val user = editing ?: Session.repo.createUser(name, email, phone, role, password)
            user.name = name; user.phone = phone; user.role = role
            user.active = binding.activeSwitch.isChecked
            user.verified = binding.verifiedSwitch.isChecked
            Session.repo.updateUser(user)
            Toast.makeText(this, if (editing == null) "Usuario creado" else "Usuario actualizado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
