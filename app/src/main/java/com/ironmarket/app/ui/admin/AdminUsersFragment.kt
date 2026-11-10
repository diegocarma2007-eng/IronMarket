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
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentListBinding

/** Gestión de usuarios: listado con filtros, creación, edición, suspensión y eliminación. */
class AdminUsersFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: UserAdapter
    private var filter: Role? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.text = "Usuarios"
        adapter = UserAdapter(emptyList(),
            onEdit = { openForm(it.id) },
            onToggle = { u ->
                if (u.id == Session.currentUser?.id) return@UserAdapter
                u.active = !u.active
                if (u.role == Role.VENDEDOR && u.active) u.verified = true
                refresh()
                Snackbar.make(binding.root, if (u.active) "${u.name} reactivado" else "${u.name} suspendido", Snackbar.LENGTH_SHORT).show()
            },
            onDelete = { u ->
                if (u.id == Session.currentUser?.id) {
                    Snackbar.make(binding.root, "No puedes eliminar tu propia cuenta", Snackbar.LENGTH_SHORT).show()
                    return@UserAdapter
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("¿Eliminar a ${u.name}?")
                    .setMessage("La cuenta se anonimiza; los pedidos y facturas se conservan.")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ -> DataRepository.deleteUser(u.id); refresh() }
                    .show()
            })
        binding.list.adapter = adapter

        val chips = listOf<Pair<String, Role?>>("Todos" to null, "Compradores" to Role.COMPRADOR, "Vendedores" to Role.VENDEDOR, "Admins" to Role.ADMIN)
        chips.forEach { (label, role) ->
            val chip = Chip(requireContext()).apply { text = label; isCheckable = true; isChecked = role == null }
            chip.setOnClickListener { filter = role; refresh() }
            binding.filterChips.addView(chip)
        }
        binding.fab.visibility = View.VISIBLE
        binding.fab.text = getString(R.string.new_user)
        binding.fab.setOnClickListener { openForm(null) }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun openForm(id: Int?) {
        val intent = Intent(requireContext(), UserFormActivity::class.java)
        id?.let { intent.putExtra(UserFormActivity.EXTRA_ID, it) }
        startActivity(intent)
    }

    private fun refresh() {
        val all = DataRepository.users
        val shown = all.filter { filter == null || it.role == filter }
        adapter.submit(shown)
        binding.subtitle.text = "${all.size} usuarios · ${all.count { !it.active }} suspendidos"
        binding.emptyView.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
