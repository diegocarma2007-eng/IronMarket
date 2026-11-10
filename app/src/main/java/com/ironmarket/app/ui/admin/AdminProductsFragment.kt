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
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.databinding.FragmentListBinding
import com.ironmarket.app.ui.seller.ProductFormActivity
import com.ironmarket.app.ui.seller.ProductRowAdapter

/** Gestión de productos: revisar (aprobar/rechazar), crear, editar y eliminar. */
class AdminProductsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductRowAdapter
    private var filter: ProductStatus? = ProductStatus.EN_REVISION

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.text = "Productos"
        adapter = ProductRowAdapter(emptyList(), adminMode = true,
            onEdit = { p -> startActivity(Intent(requireContext(), ProductFormActivity::class.java).putExtra(ProductFormActivity.EXTRA_ID, p.id)) },
            onDelete = { p ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("¿Despublicar y eliminar «${p.name}»?")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ -> DataRepository.deleteProduct(p.id); refresh() }
                    .show()
            },
            onModerate = { p, approve ->
                p.status = if (approve) ProductStatus.PUBLICADO else ProductStatus.RECHAZADO
                refresh()
                Snackbar.make(binding.root, if (approve) "«${p.name}» publicado" else "«${p.name}» rechazado", Snackbar.LENGTH_SHORT).show()
            })
        binding.list.adapter = adapter

        val chips = listOf<Pair<String, ProductStatus?>>("En revisión" to ProductStatus.EN_REVISION, "Publicados" to ProductStatus.PUBLICADO,
            "Rechazados" to ProductStatus.RECHAZADO, "Todos" to null)
        chips.forEach { (label, status) ->
            val chip = Chip(requireContext()).apply { text = label; isCheckable = true; isChecked = status == filter }
            chip.setOnClickListener { filter = status; refresh() }
            binding.filterChips.addView(chip)
        }
        binding.fab.visibility = View.VISIBLE
        binding.fab.text = getString(R.string.new_product)
        binding.fab.setOnClickListener { startActivity(Intent(requireContext(), ProductFormActivity::class.java)) }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val all = DataRepository.products
        val shown = all.filter { filter == null || it.status == filter }
        adapter.submit(shown)
        binding.subtitle.text = "${all.size} productos · ${all.count { it.status == ProductStatus.EN_REVISION }} en revisión"
        binding.emptyView.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
