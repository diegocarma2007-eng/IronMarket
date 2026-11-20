package com.ironmarket.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ironmarket.app.R
import com.ironmarket.app.data.Product
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentListBinding
import com.ironmarket.app.ui.seller.ProductFormActivity
import com.ironmarket.app.ui.seller.ProductRowAdapter
import com.ironmarket.app.util.launchView

/** Administración de productos: revisar (aprobar/rechazar), crear, editar y eliminar. */
class AdminProductsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductRowAdapter
    private var filter: ProductStatus? = ProductStatus.EN_REVISION
    private var all: List<Product> = emptyList()

    /** Infla el layout de la revisión de productos con ViewBinding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura los controles de la revisión de productos y carga sus datos. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.text = "Productos"
        adapter = ProductRowAdapter(emptyList(), adminMode = true,
            onEdit = { p -> startActivity(Intent(requireContext(), ProductFormActivity::class.java).putExtra(ProductFormActivity.EXTRA_ID, p.id)) },
            onDelete = { p ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("¿Despublicar y eliminar «${p.name}»?")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ -> launchView { Session.repo.deleteProduct(p.id); load() } }
                    .show()
            },
            onModerate = { p, approve ->
                // Aprobar publica el producto en el catálogo; rechazar lo deja oculto
                p.status = if (approve) ProductStatus.PUBLICADO else ProductStatus.RECHAZADO
                launchView {
                    Session.repo.saveProduct(p, null); load()
                    Snackbar.make(binding.root, if (approve) "«${p.name}» publicado" else "«${p.name}» rechazado", Snackbar.LENGTH_SHORT).show()
                }
            })
        binding.list.adapter = adapter

        val chips = listOf<Pair<String, ProductStatus?>>("En revisión" to ProductStatus.EN_REVISION, "Publicados" to ProductStatus.PUBLICADO,
            "Rechazados" to ProductStatus.RECHAZADO, "Todos" to null)
        chips.forEach { (label, status) ->
            val chip = Chip(requireContext()).apply { text = label; isCheckable = true; isChecked = status == filter }
            chip.setOnClickListener { filter = status; render() }
            binding.filterChips.addView(chip)
        }
        binding.fab.visibility = View.VISIBLE
        binding.fab.text = getString(R.string.new_product)
        binding.fab.setOnClickListener { startActivity(Intent(requireContext(), ProductFormActivity::class.java)) }
    }

    /** Recarga los datos al volver a la pantalla para mostrar cambios recientes. */
    override fun onResume() { super.onResume(); launchView { load() } }

    /** Consulta los datos en el repositorio de forma asíncrona. */
    private suspend fun load() { all = Session.repo.getProducts(); render() }

    /** Aplica el filtro seleccionado y muestra la lista o el estado vacío. */
    private fun render() {
        if (_binding == null) return
        val shown = all.filter { filter == null || it.status == filter }
        adapter.submit(shown)
        binding.subtitle.text = "${all.size} productos · ${all.count { it.status == ProductStatus.EN_REVISION }} en revisión"
        binding.emptyView.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Libera la referencia al binding para evitar fugas de memoria. */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
