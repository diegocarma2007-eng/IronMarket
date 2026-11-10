package com.ironmarket.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentAdminDashboardBinding
import com.ironmarket.app.databinding.ItemLineBinding
import com.ironmarket.app.databinding.ViewStatBinding
import com.ironmarket.app.ui.auth.LoginActivity
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.toCop

/** Dashboard principal del administrador con indicadores calculados del repositorio. */
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.roleBadge.badge("Administrador", R.color.im_blue_dark, R.color.im_blue)
        binding.logoutButton.setOnClickListener {
            Session.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }

        val users = DataRepository.users
        val pendingSellers = users.count { it.role == Role.VENDEDOR && !it.verified }
        val pendingProducts = DataRepository.products.count { it.status == ProductStatus.EN_REVISION }

        stat(binding.statUsers, users.count { it.active }.toString(), "Usuarios activos", "${users.size} registrados")
        stat(binding.statSellers, users.count { it.role == Role.VENDEDOR }.toString(), "Vendedores", "$pendingSellers por verificar")
        stat(binding.statSales, DataRepository.totalSales().toCop(), "Ventas", "${DataRepository.orders.size} pedidos")
        stat(binding.statPending, pendingProducts.toString(), "En revisión", "productos pendientes")

        binding.pendingProductsButton.text = "$pendingProducts productos esperan revisión"
        binding.pendingSellersButton.text = "$pendingSellers vendedores por verificar RUT o NIT"
        binding.pendingProductsButton.setOnClickListener { (activity as AdminActivity).selectTab(R.id.nav_products) }
        binding.pendingSellersButton.setOnClickListener { (activity as AdminActivity).selectTab(R.id.nav_users) }

        listOf(
            "Producto «Pre-entreno X» rechazado: sin registro INVIMA" to "23:41",
            "Usuario suspendido por reportes de fraude" to "22:10",
            "Vendedor «La Fuerza» verificado" to "21:55",
            "Nuevo pedido #IM-10482 por ${(307_800).toCop()}" to "20:30"
        ).forEach { (text, time) ->
            val line = ItemLineBinding.inflate(layoutInflater, binding.activityLog, false)
            line.label.text = text; line.value.text = time
            binding.activityLog.addView(line.root)
        }
    }

    private fun stat(s: ViewStatBinding, value: String, label: String, hint: String) {
        s.value.text = value; s.label.text = label
        s.hint.text = hint; s.hint.visibility = View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
