package com.ironmarket.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ironmarket.app.R
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentAdminDashboardBinding
import com.ironmarket.app.databinding.ItemLineBinding
import com.ironmarket.app.databinding.ViewStatBinding
import com.ironmarket.app.ui.common.Navigation
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.launchView
import com.ironmarket.app.util.toCop
import com.ironmarket.app.util.toDate

/** Dashboard del administrador: indicadores calculados en tiempo real desde la base de datos. */
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    /** Infla el layout de el panel general del administrador con ViewBinding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura los controles de el panel general del administrador y carga sus datos. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.roleBadge.badge("Administrador", R.color.im_blue_dark, R.color.im_blue)
        binding.logoutButton.setOnClickListener { Navigation.logout(requireActivity()) }
        binding.pendingProductsButton.setOnClickListener { (activity as AdminActivity).selectTab(R.id.nav_products) }
        binding.pendingSellersButton.setOnClickListener { (activity as AdminActivity).selectTab(R.id.nav_users) }
        binding.sourceLabel.text = "Base de datos: ${Session.repo.sourceName}"
        binding.seedButton.setOnClickListener { seed() }
        launchView { load() }
    }

    /** Consulta usuarios, productos y órdenes y calcula los indicadores. */
    private suspend fun load() {
        val users = Session.repo.getUsers()
        val products = Session.repo.getProducts()
        val orders = Session.repo.getOrders()
        if (_binding == null) return
        val pendingSellers = users.count { it.role == Role.VENDEDOR && !it.verified }
        val pendingProducts = products.count { it.status == ProductStatus.EN_REVISION }

        stat(binding.statUsers, users.count { it.active }.toString(), "Usuarios activos", "${users.size} registrados")
        stat(binding.statSellers, users.count { it.role == Role.VENDEDOR }.toString(), "Vendedores", "$pendingSellers por verificar")
        stat(binding.statSales, orders.sumOf { it.total }.toCop(), "Ventas", "${orders.size} órdenes")
        stat(binding.statPending, pendingProducts.toString(), "En revisión", "${products.size} productos en total")

        binding.pendingProductsButton.text = "$pendingProducts productos esperan revisión"
        binding.pendingSellersButton.text = "$pendingSellers vendedores por verificar RUT o NIT"
        binding.seedButton.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE

        binding.activityLog.removeAllViews()
        val recent = orders.take(5)
        if (recent.isEmpty()) addLine("Aún no hay órdenes registradas", "")
        recent.forEach { addLine("${it.buyerName}: ${it.summary}", "${it.total.toCop()} · ${it.createdAt.toDate()}") }
    }

    /** Agrega una fila de orden reciente al panel. */
    private fun addLine(text: String, value: String) {
        val line = ItemLineBinding.inflate(layoutInflater, binding.activityLog, false)
        line.label.text = text; line.value.text = value
        binding.activityLog.addView(line.root)
    }

    /** Carga productos de ejemplo asignados al primer vendedor registrado (o al administrador). */
    private fun seed() = launchView(binding.seedButton) {
        val seller = Session.repo.getUsers().firstOrNull { it.role == Role.VENDEDOR } ?: Session.currentUser!!
        val n = Session.repo.seedDemoProducts(seller)
        Toast.makeText(requireContext(), "$n productos de ejemplo creados para ${seller.name}", Toast.LENGTH_LONG).show()
        load()
    }

    /** Llena una tarjeta de indicador con valor y etiqueta. */
    private fun stat(s: ViewStatBinding, value: String, label: String, hint: String) {
        s.value.text = value; s.label.text = label
        s.hint.text = hint; s.hint.visibility = View.VISIBLE
    }

    /** Libera la referencia al binding para evitar fugas de memoria. */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
