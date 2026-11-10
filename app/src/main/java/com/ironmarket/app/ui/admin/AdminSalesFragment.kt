package com.ironmarket.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ironmarket.app.data.Category
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.databinding.FragmentAdminSalesBinding
import com.ironmarket.app.databinding.ItemBarBinding
import com.ironmarket.app.databinding.ItemLineBinding
import com.ironmarket.app.util.toCop

/** Reporte de ventas: total, pedidos, ticket promedio, ventas por categoría y top de productos. */
class AdminSalesFragment : Fragment() {

    private var _binding: FragmentAdminSalesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAdminSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val orders = DataRepository.orders
        val total = orders.sumOf { it.total }
        binding.totalSales.text = total.toCop()
        binding.statOrders.value.text = orders.size.toString()
        binding.statOrders.label.text = "Pedidos"
        binding.statTicket.value.text = (if (orders.isEmpty()) 0 else total / orders.size).toCop()
        binding.statTicket.label.text = "Ticket promedio"

        // Barras por categoría, proporcionales a la categoría que más vende
        val byCategory = Category.entries.associateWith { c -> orders.filter { it.category == c }.sumOf { it.total } }
        val max = byCategory.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        byCategory.entries.sortedByDescending { it.value }.forEach { (cat, value) ->
            val row = ItemBarBinding.inflate(layoutInflater, binding.categoryBars, false)
            row.label.text = cat.label
            row.value.text = value.toCop()
            row.bar.max = 100
            row.bar.setProgressCompat(value * 100 / max, true)
            binding.categoryBars.addView(row.root)
        }

        DataRepository.products.sortedByDescending { it.sold }.take(5).forEachIndexed { i, p ->
            val line = ItemLineBinding.inflate(layoutInflater, binding.topProducts, false)
            line.label.text = "${i + 1}. ${p.name}"
            line.value.text = "${p.sold} und."
            binding.topProducts.addView(line.root)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
