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
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentBuyerHomeBinding
import com.ironmarket.app.util.launchView

/** Inicio del comprador: saludo, ubicación, búsqueda, categorías, banner y más vendidos (Firestore). */
class BuyerHomeFragment : Fragment() {

    private var _binding: FragmentBuyerHomeBinding? = null
    private val binding get() = _binding!!

    /** Infla el layout de el inicio del comprador con ViewBinding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentBuyerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura los controles de el inicio del comprador y carga sus datos. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = Session.currentUser
        binding.greeting.text = "Hola, ${user?.name?.split(" ")?.first().orEmpty()}"
        // Muestra barrio y ciudad de la dirección guardada (últimas dos partes)
        binding.location.text = user?.address?.takeIf { it.isNotBlank() }
            ?.split(", ")?.takeLast(2)?.joinToString(", ") ?: "Agrega tu dirección en Perfil"

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

        val width = (resources.displayMetrics.widthPixels * 0.44).toInt()
        val adapter = ProductGridAdapter(emptyList(), width) { openDetail(it.id) }
        binding.bestSellersList.adapter = adapter
        launchView {
            // Más vendidos: productos publicados ordenados por unidades vendidas
            adapter.submit(Session.repo.getProducts(ProductStatus.PUBLICADO).sortedByDescending { it.sold }.take(6))
        }

        binding.seeAllButton.setOnClickListener { openCatalog() }
        binding.nearbyBanner.setOnClickListener { openCatalog(category = Category.EQUIPO) }
        binding.notificationsButton.setOnClickListener {
            Toast.makeText(requireContext(), "No tienes notificaciones nuevas", Toast.LENGTH_SHORT).show()
        }
    }

    /** Abre el catálogo con la búsqueda o categoría elegida. */
    private fun openCatalog(query: String? = null, category: Category? = null) {
        CatalogFragment.pendingQuery = query
        CatalogFragment.pendingCategory = category
        (activity as? BuyerActivity)?.selectTab(R.id.nav_catalog)
    }

    /** Abre el detalle del producto. */
    private fun openDetail(id: String) =
        startActivity(Intent(requireContext(), ProductDetailActivity::class.java).putExtra(ProductDetailActivity.EXTRA_ID, id))

    /** Libera la referencia al binding para evitar fugas de memoria. */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
