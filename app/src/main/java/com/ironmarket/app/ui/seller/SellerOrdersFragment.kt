package com.ironmarket.app.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.OrderStatus
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentListBinding
import com.ironmarket.app.util.toCop

/** Historial de pedidos recibidos por el vendedor, filtrado por estado. */
class SellerOrdersFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OrderAdapter
    private var filter: OrderStatus? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.text = "Pedidos recibidos"
        adapter = OrderAdapter(emptyList()) { order ->
            order.status = if (order.status == OrderStatus.NUEVO) OrderStatus.EN_CAMINO else OrderStatus.ENTREGADO
            refresh()
            Snackbar.make(binding.root, "Pedido #${order.id}: ${order.status.label}", Snackbar.LENGTH_SHORT).show()
        }
        binding.list.adapter = adapter
        val chips = listOf<Pair<String, OrderStatus?>>("Todos" to null) + OrderStatus.entries.map { it.label to it }
        chips.forEach { (label, status) ->
            val chip = Chip(requireContext()).apply { text = label; isCheckable = true; isChecked = status == null }
            chip.setOnClickListener { filter = status; refresh() }
            binding.filterChips.addView(chip)
        }
        refresh()
    }

    private fun refresh() {
        val all = DataRepository.ordersOfSeller(Session.currentUser?.id ?: return)
        val shown = all.filter { filter == null || it.status == filter }
        adapter.submit(shown)
        binding.subtitle.text = "${all.count { it.status == OrderStatus.NUEVO }} por despachar · ${all.sumOf { it.total }.toCop()} en ventas"
        binding.emptyView.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
