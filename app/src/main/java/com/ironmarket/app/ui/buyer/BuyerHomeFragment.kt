package com.ironmarket.app.ui.buyer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.ironmarket.app.R
import com.ironmarket.app.data.Category
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentBuyerHomeBinding

/** Inicio del comprador: saludo, búsqueda, categorías, banner y más vendidos. */
class BuyerHomeFragment : Fragment() {

    private var _binding: FragmentBuyerHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentBuyerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val firstName = Session.currentUser?.name?.split(" ")?.first() ?: ""
        binding.greeting.text = "Hola, $firstName"

        // Categorías: al tocar una se abre el catálogo filtrado
        Category.entries.forEach { cat ->
            val chip = Chip(requireContext()).apply { text = cat.label; isCheckable = false }
            chip.setOnClickListener { openCatalog(category = cat) }
            binding.categoryChips.addView(chip)
        }

        // Búsqueda: al presionar "buscar" en el teclado
        binding.searchInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { openCatalog(query = v.text.toString()); true } else false
        }

        // Más vendidos: lista horizontal ordenada por unidades vendidas
        val best = DataRepository.publishedProducts().sortedByDescending { it.sold }.take(6)
        val width = (resources.displayMetrics.widthPixels * 0.44).toInt()
        binding.bestSellersList.adapter = ProductGridAdapter(best, width) { openDetail(it.id) }

        binding.seeAllButton.setOnClickListener { openCatalog() }
        binding.nearbyBanner.setOnClickListener { openCatalog(category = Category.EQUIPO) }
        binding.notificationsButton.setOnClickListener {
            Toast.makeText(requireContext(), "No tienes notificaciones nuevas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCatalog(query: String? = null, category: Category? = null) {
        CatalogFragment.pendingQuery = query
        CatalogFragment.pendingCategory = category
        (activity as? BuyerActivity)?.selectTab(R.id.nav_catalog)
    }

    private fun openDetail(id: Int) =
        startActivity(Intent(requireContext(), ProductDetailActivity::class.java).putExtra(ProductDetailActivity.EXTRA_ID, id))

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
