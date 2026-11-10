package com.ironmarket.app.ui.common

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.OrderStatus
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentProfileBinding
import com.ironmarket.app.databinding.ItemOptionBinding
import com.ironmarket.app.ui.auth.LoginActivity
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.initials
import com.ironmarket.app.util.toCop

/** Perfil de usuario (comprador) o de vendedor. CRUD de Perfil: ver, editar y eliminar cuenta. */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindUser()
        binding.editButton.setOnClickListener { showEditDialog() }
        binding.biometricSwitch.isChecked = Session.isBiometricEnabled(requireContext())
        binding.biometricSwitch.setOnCheckedChangeListener { _, checked ->
            Session.setBiometricEnabled(requireContext(), checked)
        }
        binding.logoutButton.setOnClickListener {
            Session.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        buildOptions()
    }

    private fun bindUser() {
        val user = Session.currentUser ?: return
        binding.avatar.text = user.name.initials()
        binding.name.text = user.name
        binding.email.text = user.email
        if (user.role == Role.VENDEDOR) {
            val label = if (user.verified) "Vendedor verificado" else "Vendedor · verificación pendiente"
            binding.roleBadge.badge(label, R.color.im_orange_dark, R.color.im_orange)
            val products = DataRepository.productsOfSeller(user.id)
            val orders = DataRepository.ordersOfSeller(user.id)
            setStat(binding.stat1.value, binding.stat1.label, products.size.toString(), "Productos")
            setStat(binding.stat2.value, binding.stat2.label, orders.size.toString(), "Pedidos")
            setStat(binding.stat3.value, binding.stat3.label, (orders.sumOf { it.total } / 1000).let { "$${it}k" }, "Ventas")
        } else {
            binding.roleBadge.badge("Comprador", R.color.im_lime_dark, R.color.im_lime)
            val mine = DataRepository.orders.filter { it.buyerName.startsWith(user.name.split(" ").first()) }
            setStat(binding.stat1.value, binding.stat1.label, mine.size.toString(), "Pedidos")
            setStat(binding.stat2.value, binding.stat2.label, mine.count { it.status != OrderStatus.ENTREGADO }.toString(), "En camino")
            setStat(binding.stat3.value, binding.stat3.label, DataRepository.cartCount().toString(), "En carrito")
        }
    }

    private fun setStat(value: android.widget.TextView, label: android.widget.TextView, v: String, l: String) {
        value.text = v; label.text = l
    }

    private fun buildOptions() {
        val seller = Session.role == Role.VENDEDOR
        val options = if (seller) listOf(
            R.drawable.ic_store to "Datos de la tienda",
            R.drawable.ic_location to "Dirección de despacho",
            R.drawable.ic_bank to "Cuenta para recibir pagos",
            R.drawable.ic_shield to "Privacidad y mis datos",
            R.drawable.ic_delete to "Eliminar cuenta"
        ) else listOf(
            R.drawable.ic_orders to "Mis pedidos",
            R.drawable.ic_location to "Direcciones guardadas",
            R.drawable.ic_card to "Métodos de pago",
            R.drawable.ic_shield to "Privacidad y mis datos",
            R.drawable.ic_delete to "Eliminar cuenta"
        )
        options.forEach { (icon, label) ->
            val row = ItemOptionBinding.inflate(layoutInflater, binding.optionsGroup, false)
            row.icon.setImageResource(icon)
            row.label.text = label
            row.root.setOnClickListener { onOption(label) }
            binding.optionsGroup.addView(row.root)
        }
    }

    private fun onOption(label: String) {
        when (label) {
            "Datos de la tienda" -> showEditDialog()
            "Mis pedidos" -> {
                val user = Session.currentUser ?: return
                val mine = DataRepository.orders.filter { it.buyerName.startsWith(user.name.split(" ").first()) }
                val text = if (mine.isEmpty()) "Aún no tienes pedidos."
                else mine.joinToString("\n\n") { "#${it.id} · ${it.status.label}\n${it.summary}\n${it.total.toCop()}" }
                MaterialAlertDialogBuilder(requireContext()).setTitle("Mis pedidos").setMessage(text)
                    .setPositiveButton("Cerrar", null).show()
            }
            "Eliminar cuenta" -> MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Eliminar tu cuenta?")
                .setMessage("Se suprimirán tus datos personales (Ley 1581 de 2012). Las facturas se conservan por obligación legal.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    Session.currentUser?.let { DataRepository.deleteUser(it.id) }
                    binding.logoutButton.performClick()
                }.show()
            else -> Toast.makeText(requireContext(), "$label: disponible en la siguiente fase", Toast.LENGTH_SHORT).show()
        }
    }

    /** Editar perfil (U del CRUD de Perfil): nombre y celular. */
    private fun showEditDialog() {
        val user = Session.currentUser ?: return
        val ctx = requireContext()
        val pad = resources.getDimensionPixelSize(R.dimen.space_xl)
        val form = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad / 2, pad, 0) }
        fun field(hint: String, value: String): TextInputEditText {
            val layout = TextInputLayout(ctx).apply { this.hint = hint }
            val input = TextInputEditText(layout.context).apply { setText(value) }
            layout.addView(input); form.addView(layout)
            return input
        }
        val name = field(if (user.role == Role.VENDEDOR) "Nombre de la tienda" else "Nombre", user.name)
        val phone = field("Celular", user.phone)
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Editar perfil")
            .setView(form)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                if (name.text.isNullOrBlank()) return@setPositiveButton
                user.name = name.text.toString().trim()
                user.phone = phone.text.toString().trim()
                DataRepository.updateUser(user)
                bindUser()
                Toast.makeText(ctx, "Perfil actualizado", Toast.LENGTH_SHORT).show()
            }.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
