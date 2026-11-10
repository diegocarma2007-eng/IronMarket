package com.ironmarket.app.ui.buyer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.ironmarket.app.data.Category
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.databinding.FragmentCatalogBinding
import java.text.Normalizer

/** Catálogo de productos con búsqueda en vivo y filtro por categoría. */
class CatalogFragment : Fragment() {

    companion object {
        // Filtros enviados desde el Inicio
        var pendingQuery: String? = null
        var pendingCategory: Category? = null
    }

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductGridAdapter
    private var category: Category? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ProductGridAdapter(emptyList()) { p ->
            startActivity(Intent(requireContext(), ProductDetailActivity::class.java).putExtra(ProductDetailActivity.EXTRA_ID, p.id))
        }
        binding.productList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.productList.adapter = adapter

        category = pendingCategory
        // Chip "Todo" + un chip por categoría
        val all = Chip(requireContext()).apply { text = "Todo"; isCheckable = true; isChecked = category == null }
        all.setOnClickListener { category = null; filter() }
        binding.categoryChips.addView(all)
        Category.entries.forEach { cat ->
            val chip = Chip(requireContext()).apply { text = cat.label; isCheckable = true; isChecked = cat == category }
            chip.setOnClickListener { category = cat; filter() }
            binding.categoryChips.addView(chip)
        }

        pendingQuery?.let { binding.searchInput.setText(it) }
        pendingQuery = null; pendingCategory = null
        binding.searchInput.doAfterTextChanged { filter() }
        filter()
    }

    private fun normalize(s: String) =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "")

    private fun filter() {
        val q = normalize(binding.searchInput.text?.toString().orEmpty().trim())
        val result = DataRepository.publishedProducts().filter {
            (category == null || it.category == category) && (q.isEmpty() || normalize(it.name).contains(q))
        }
        adapter.submit(result)
        binding.resultCount.text = "${result.size} productos"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
