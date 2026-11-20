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
import com.ironmarket.app.data.Product
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentCatalogBinding
import com.ironmarket.app.util.launchView
import java.text.Normalizer

/** Catálogo: carga los productos publicados y filtra en vivo por texto y categoría. */
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
    private var all: List<Product> = emptyList()

    /** Infla el layout de el catálogo con ViewBinding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura los controles de el catálogo y carga sus datos. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ProductGridAdapter(emptyList()) { p ->
            startActivity(Intent(requireContext(), ProductDetailActivity::class.java).putExtra(ProductDetailActivity.EXTRA_ID, p.id))
        }
        binding.productList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.productList.adapter = adapter

        category = pendingCategory
        val allChip = Chip(requireContext()).apply { text = "Todo"; isCheckable = true; isChecked = category == null }
        allChip.setOnClickListener { category = null; filter() }
        binding.categoryChips.addView(allChip)
        Category.entries.forEach { cat ->
            val chip = Chip(requireContext()).apply { text = cat.label; isCheckable = true; isChecked = cat == category }
            chip.setOnClickListener { category = cat; filter() }
            binding.categoryChips.addView(chip)
        }

        pendingQuery?.let { binding.searchInput.setText(it) }
        pendingQuery = null; pendingCategory = null
        binding.searchInput.doAfterTextChanged { filter() }

        binding.resultCount.text = "Cargando productos…"
        launchView {
            all = Session.repo.getProducts(ProductStatus.PUBLICADO)
            filter()
        }
    }

    /** Quita tildes y pasa a minúsculas para comparar búsquedas. */
    private fun normalize(s: String) =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "")

    /** Aplica el texto de búsqueda y la categoría elegida a la lista cargada. */
    private fun filter() {
        val q = normalize(binding.searchInput.text?.toString().orEmpty().trim().take(100))
        val result = all.filter {
            (category == null || it.category == category) && (q.isEmpty() || normalize(it.name).contains(q))
        }
        adapter.submit(result)
        binding.resultCount.text = "${result.size} productos"
    }

    /** Libera la referencia al binding para evitar fugas de memoria. */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
