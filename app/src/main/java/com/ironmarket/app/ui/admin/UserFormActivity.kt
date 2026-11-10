package com.ironmarket.app.ui.admin

import android.os.Bundle
import android.widget.Toast
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.User
import com.ironmarket.app.databinding.ActivityUserFormBinding
import com.ironmarket.app.ui.common.RoleActivity
import com.ironmarket.app.util.isValidEmail

/** Crear (C) y editar (U) usuarios desde la administración. */
class UserFormActivity : RoleActivity() {

    companion object { const val EXTRA_ID = "user_id" }

    private lateinit var binding: ActivityUserFormBinding
    private var editing: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editing = DataRepository.findUser(intent.getIntExtra(EXTRA_ID, -1))
        binding.header.formTitle.text = getString(if (editing == null) R.string.new_user else R.string.edit_user)
        binding.header.closeButton.setOnClickListener { finish() }

        editing?.let { u ->
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
        binding.saveButton.setOnClickListener { save() }
    }

    private fun save() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        binding.nameLayout.error = null; binding.emailLayout.error = null

        var ok = true
        if (name.length < 3) { binding.nameLayout.error = getString(R.string.error_required); ok = false }
        if (!email.isValidEmail()) { binding.emailLayout.error = getString(R.string.error_email); ok = false }
        val other = DataRepository.findUserByEmail(email)
        if (other != null && other.id != editing?.id) { binding.emailLayout.error = "Correo ya registrado"; ok = false }
        if (!ok) return

        val role = when (binding.roleToggle.checkedButtonId) {
            R.id.roleSeller -> Role.VENDEDOR
            R.id.roleAdmin -> Role.ADMIN
            else -> Role.COMPRADOR
        }
        val user = editing ?: DataRepository.createUser(name, email, phone, role)
        user.name = name; user.email = email; user.phone = phone; user.role = role
        user.active = binding.activeSwitch.isChecked
        user.verified = binding.verifiedSwitch.isChecked
        DataRepository.updateUser(user)
        Toast.makeText(this, if (editing == null) "Usuario creado" else "Usuario actualizado", Toast.LENGTH_SHORT).show()
        finish()
    }
}
