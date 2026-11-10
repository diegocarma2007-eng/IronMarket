package com.ironmarket.app.ui.buyer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.databinding.FragmentCartBinding
import com.ironmarket.app.util.toCop

/** CRUD de Carrito: ver ítems, cambiar cantidades, eliminar, vaciar e ir a pagar. */
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.cartList.adapter = CartAdapter(DataRepository.cart) { updateSummary() }
        binding.clearButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Vaciar el carrito?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Vaciar") { _, _ ->
                    DataRepository.cart.clear()
                    binding.cartList.adapter?.notifyDataSetChanged()
                    updateSummary()
                }.show()
        }
        binding.payButton.setOnClickListener {
            startActivity(Intent(requireContext(), CheckoutActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.cartList.adapter?.notifyDataSetChanged()
        updateSummary()
    }

    private fun updateSummary() {
        val count = DataRepository.cartCount()
        val empty = DataRepository.cart.isEmpty()
        binding.title.text = if (empty) getString(R.string.cart_title) else "${getString(R.string.cart_title)} ($count)"
        binding.emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        binding.summary.visibility = if (empty) View.GONE else View.VISIBLE
        binding.clearButton.visibility = if (empty) View.GONE else View.VISIBLE
        binding.subtotalLabel.text = "Subtotal ($count productos)"
        binding.subtotal.text = DataRepository.cartSubtotal().toCop()
        binding.total.text = DataRepository.cartSubtotal().toCop()
        (activity as? BuyerActivity)?.refreshCartBadge()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
