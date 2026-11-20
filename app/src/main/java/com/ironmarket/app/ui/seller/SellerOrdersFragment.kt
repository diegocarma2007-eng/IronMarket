package com.ironmarket.app.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.ironmarket.app.data.Order
import com.ironmarket.app.data.OrderStatus
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentListBinding
import com.ironmarket.app.util.launchView
import com.ironmarket.app.util.toCop

/** Historial de pedidos recibidos por el vendedor (Firestore), filtrado por estado. */
class SellerOrdersFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OrderAdapter
    private var filter: OrderStatus? = null
    private var all: List<Order> = emptyList()

    /** Infla el layout de los pedidos del vendedor con ViewBinding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura los controles de los pedidos del vendedor y carga sus datos. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.text = "Pedidos recibidos"
        adapter = OrderAdapter(emptyList()) { order ->
            // Avanza el estado: Por despachar -> En camino -> Entregado
            val next = if (order.status == OrderStatus.NUEVO) OrderStatus.EN_CAMINO else OrderStatus.ENTREGADO
            launchView {
                Session.repo.updateOrderStatus(order.id, next)
                load()
                Snackbar.make(binding.root, "Pedido actualizado: ${next.label}", Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.list.adapter = adapter
        val chips = listOf<Pair<String, OrderStatus?>>("Todos" to null) + OrderStatus.entries.map { it.label to it }
        chips.forEach { (label, status) ->
            val chip = Chip(requireContext()).apply { text = label; isCheckable = true; isChecked = status == null }
            chip.setOnClickListener { filter = status; render() }
            binding.filterChips.addView(chip)
        }
        launchView { load() }
    }

    /** Consulta los datos en el repositorio de forma asíncrona. */
    private suspend fun load() {
        all = Session.repo.getOrders(sellerId = Session.currentUser?.id ?: return)
        render()
    }

    /** Aplica el filtro seleccionado y muestra la lista o el estado vacío. */
    private fun render() {
        if (_binding == null) return
        val shown = all.filter { filter == null || it.status == filter }
        adapter.submit(shown)
        binding.subtitle.text = "${all.count { it.status == OrderStatus.NUEVO }} por despachar · ${all.sumOf { it.total }.toCop()} en ventas"
        binding.emptyView.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Libera la referencia al binding para evitar fugas de memoria. */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
