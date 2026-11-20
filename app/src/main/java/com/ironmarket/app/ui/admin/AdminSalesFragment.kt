package com.ironmarket.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ironmarket.app.data.Category
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentAdminSalesBinding
import com.ironmarket.app.databinding.ItemBarBinding
import com.ironmarket.app.databinding.ItemLineBinding
import com.ironmarket.app.util.launchView
import com.ironmarket.app.util.toCop

/** Reporte de ventas: total, órdenes, ticket promedio, ventas por categoría y top de productos. */
class AdminSalesFragment : Fragment() {

    private var _binding: FragmentAdminSalesBinding? = null
    private val binding get() = _binding!!

    /** Infla el layout de el reporte de ventas con ViewBinding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAdminSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura los controles de el reporte de ventas y carga sus datos. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        launchView {
            val orders = Session.repo.getOrders()
            val products = Session.repo.getProducts()
            if (_binding == null) return@launchView
            val total = orders.sumOf { it.total }
            binding.totalSales.text = total.toCop()
            binding.statOrders.value.text = orders.size.toString()
            binding.statOrders.label.text = "Órdenes"
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

            products.sortedByDescending { it.sold }.take(5).forEachIndexed { i, p ->
                val line = ItemLineBinding.inflate(layoutInflater, binding.topProducts, false)
                line.label.text = "${i + 1}. ${p.name}"
                line.value.text = "${p.sold} und."
                binding.topProducts.addView(line.root)
            }
        }
    }

    /** Libera la referencia al binding para evitar fugas de memoria. */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
