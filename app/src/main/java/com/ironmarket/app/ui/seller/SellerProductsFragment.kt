package com.ironmarket.app.ui.seller

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
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentListBinding

/** Panel de productos del vendedor: CRUD completo (crear, listar, editar, eliminar). */
class SellerProductsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductRowAdapter
    private var filter: ProductStatus? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.text = "Mis productos"
        adapter = ProductRowAdapter(emptyList(), adminMode = false,
            onEdit = { openForm(it.id) },
            onDelete = { p ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("¿Eliminar «${p.name}»?")
                    .setMessage("El producto se quitará del catálogo y de los carritos.")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        DataRepository.deleteProduct(p.id); refresh()
                        Snackbar.make(binding.root, "Producto eliminado", Snackbar.LENGTH_SHORT).show()
                    }.show()
            })
        binding.list.adapter = adapter

        val chips = listOf<Pair<String, ProductStatus?>>("Todos" to null) + ProductStatus.entries.map { it.label to it }
        chips.forEach { (label, status) ->
            val chip = Chip(requireContext()).apply { text = label; isCheckable = true; isChecked = status == null }
            chip.setOnClickListener { filter = status; refresh() }
            binding.filterChips.addView(chip)
        }

        binding.fab.visibility = View.VISIBLE
        binding.fab.text = getString(R.string.new_product)
        binding.fab.setOnClickListener { openForm(null) }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun openForm(id: Int?) {
        val intent = Intent(requireContext(), ProductFormActivity::class.java)
        id?.let { intent.putExtra(ProductFormActivity.EXTRA_ID, it) }
        startActivity(intent)
    }

    private fun refresh() {
        val sellerId = Session.currentUser?.id ?: return
        val all = DataRepository.productsOfSeller(sellerId)
        val shown = all.filter { filter == null || it.status == filter }
        adapter.submit(shown)
        binding.subtitle.text = "${all.size} productos · ${all.count { it.status == ProductStatus.EN_REVISION }} en revisión · ${all.count { it.stock <= 3 }} con stock bajo"
        binding.emptyView.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
