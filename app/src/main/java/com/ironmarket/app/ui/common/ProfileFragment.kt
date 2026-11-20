package com.ironmarket.app.ui.common

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ironmarket.app.R
import com.ironmarket.app.data.OrderStatus
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentProfileBinding
import com.ironmarket.app.databinding.ItemOptionBinding
import com.ironmarket.app.util.LocationHelper
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.initials
import com.ironmarket.app.util.launchView
import com.ironmarket.app.util.toCop
import com.ironmarket.app.util.toDate

/**
 * Perfil de usuario (comprador) y de vendedor. CRUD de Perfil:
 * ver (R), editar datos y dirección con geolocalización (U) y eliminar la cuenta (D).
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    /** Permiso de ubicación para guardar la dirección actual. */
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) saveCurrentAddress()
        else Toast.makeText(requireContext(), "Sin permiso de ubicación", Toast.LENGTH_SHORT).show()
    }

    /** Infla el layout de el perfil con ViewBinding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura los controles de el perfil y carga sus datos. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindUser()
        binding.editButton.setOnClickListener { showEditDialog() }
        binding.biometricSwitch.isChecked = Session.isBiometricEnabled(requireContext())
        binding.biometricSwitch.setOnCheckedChangeListener { _, checked -> Session.setBiometricEnabled(requireContext(), checked) }
        binding.logoutButton.setOnClickListener { Navigation.logout(requireActivity()) }
        buildOptions()
    }

    /** Muestra los datos del usuario y sus indicadores (pedidos o ventas). */
    private fun bindUser() {
        val user = Session.currentUser ?: return
        binding.avatar.text = user.name.initials()
        binding.name.text = user.name
        binding.email.text = user.email
        launchView {
            if (user.role == Role.VENDEDOR) {
                binding.roleBadge.badge(if (user.verified) "Vendedor verificado" else "Vendedor · verificación pendiente",
                    R.color.im_orange_dark, R.color.im_orange)
                val products = Session.repo.getProducts(sellerId = user.id)
                val orders = Session.repo.getOrders(sellerId = user.id)
                setStat(binding.stat1.value, binding.stat1.label, products.size.toString(), "Productos")
                setStat(binding.stat2.value, binding.stat2.label, orders.size.toString(), "Pedidos")
                setStat(binding.stat3.value, binding.stat3.label, "$${orders.sumOf { it.total } / 1000}k", "Ventas")
            } else {
                binding.roleBadge.badge("Comprador", R.color.im_lime_dark, R.color.im_lime)
                val orders = Session.repo.getOrders(buyerId = user.id)
                setStat(binding.stat1.value, binding.stat1.label, orders.size.toString(), "Pedidos")
                setStat(binding.stat2.value, binding.stat2.label, orders.count { it.status != OrderStatus.ENTREGADO }.toString(), "En camino")
                setStat(binding.stat3.value, binding.stat3.label, Session.repo.getCart(user.id).sumOf { it.quantity }.toString(), "En carrito")
            }
        }
    }

    /** Llena un indicador del perfil. */
    private fun setStat(value: TextView, label: TextView, v: String, l: String) { value.text = v; label.text = l }

    /** Construye la lista de opciones según el rol. */
    private fun buildOptions() {
        val seller = Session.role == Role.VENDEDOR
        val options = if (seller) listOf(
            R.drawable.ic_store to "Datos de la tienda",
            R.drawable.ic_location to "Dirección de despacho",
            R.drawable.ic_shield to "Privacidad y mis datos",
            R.drawable.ic_delete to "Eliminar cuenta"
        ) else listOf(
            R.drawable.ic_orders to "Mis pedidos",
            R.drawable.ic_location to "Dirección de entrega",
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

    /** Ejecuta la opción del perfil tocada (pedidos, dirección, privacidad o eliminar cuenta). */
    private fun onOption(label: String) {
        val user = Session.currentUser ?: return
        when (label) {
            "Datos de la tienda" -> showEditDialog()
            "Mis pedidos" -> launchView {
                val mine = Session.repo.getOrders(buyerId = user.id)
                val text = if (mine.isEmpty()) "Aún no tienes pedidos."
                else mine.joinToString("\n\n") { "${it.createdAt.toDate()} · ${it.status.label}\n${it.summary}\n${it.total.toCop()} · Ref. ${it.paymentRef}" }
                MaterialAlertDialogBuilder(requireContext()).setTitle("Mis pedidos").setMessage(text)
                    .setPositiveButton("Cerrar", null).show()
            }
            "Dirección de entrega", "Dirección de despacho" -> MaterialAlertDialogBuilder(requireContext())
                .setTitle(label)
                .setMessage(user.address.ifBlank { "Aún no tienes una dirección guardada." })
                .setNegativeButton("Cerrar", null)
                .setPositiveButton("Usar mi ubicación actual") { _, _ ->
                    locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }.show()
            "Privacidad y mis datos" -> MaterialAlertDialogBuilder(requireContext())
                .setTitle("Privacidad y mis datos")
                .setMessage("Tus datos se tratan según la Ley 1581 de 2012. Puedes consultarlos y actualizarlos aquí, " +
                    "revocar el ingreso con huella desde el interruptor y solicitar la supresión eliminando tu cuenta. " +
                    "La huella nunca sale de tu dispositivo.\n\nDatos guardados:\n• ${user.name}\n• ${user.email}\n• ${user.phone}\n• ${user.address.ifBlank { "Sin dirección" }}")
                .setPositiveButton("Cerrar", null).show()
            "Eliminar cuenta" -> MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Eliminar tu cuenta?")
                .setMessage("Se suprimirán tus datos personales (Ley 1581 de 2012). Las facturas y órdenes se conservan por obligación legal.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    launchView { Session.repo.deleteUser(user.id); Navigation.logout(requireActivity()) }
                }.show()
        }
    }

    /** Obtiene la ubicación actual y la guarda como dirección del usuario (U). */
    private fun saveCurrentAddress() = launchView {
        Toast.makeText(requireContext(), "Buscando tu ubicación…", Toast.LENGTH_SHORT).show()
        val place = LocationHelper.currentPlace(requireContext())
        if (place == null) { Toast.makeText(requireContext(), "Activa el GPS e inténtalo de nuevo", Toast.LENGTH_LONG).show(); return@launchView }
        val user = Session.currentUser ?: return@launchView
        user.address = place.address
        Session.repo.updateUser(user)
        Toast.makeText(requireContext(), "Dirección guardada: ${place.address}", Toast.LENGTH_LONG).show()
    }

    /** Editar perfil (U del CRUD de Perfil): nombre y celular. */
    private fun showEditDialog() {
        val user = Session.currentUser ?: return
        val ctx = requireContext()
        val pad = resources.getDimensionPixelSize(R.dimen.space_xl)
        val form = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad / 2, pad, 0) }
        /** Crea un campo de texto para el diálogo de edición. */
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
                launchView {
                    Session.repo.updateUser(user)
                    bindUser()
                    Toast.makeText(ctx, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    /** Libera la referencia al binding para evitar fugas de memoria. */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
