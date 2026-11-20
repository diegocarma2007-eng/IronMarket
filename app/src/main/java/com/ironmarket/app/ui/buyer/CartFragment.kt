package com.ironmarket.app.ui.buyer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ironmarket.app.R
import com.ironmarket.app.data.CartItem
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.FragmentCartBinding
import com.ironmarket.app.util.launchView
import com.ironmarket.app.util.toCop

/** CRUD de Carrito: ver ítems, cambiar cantidades, eliminar, vaciar e ir a pagar. Persistido en Firestore. */
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CartAdapter
    private var items: List<CartItem> = emptyList()
    private val uid get() = Session.currentUser?.id.orEmpty()

    /** Infla el layout de el carrito de compras con ViewBinding. */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Configura los controles de el carrito de compras y carga sus datos. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = CartAdapter(emptyList()) { item, qty ->
            // Actualiza (U) o elimina (D) el ítem en la base de datos y recarga
            launchView { Session.repo.setCartQuantity(uid, item.product.id, qty); load() }
        }
        binding.cartList.adapter = adapter
        binding.clearButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Vaciar el carrito?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Vaciar") { _, _ -> launchView { Session.repo.clearCart(uid); load() } }
                .show()
        }
        binding.payButton.setOnClickListener { startActivity(Intent(requireContext(), CheckoutActivity::class.java)) }
    }

    /** Recarga los datos al volver a la pantalla para mostrar cambios recientes. */
    override fun onResume() { super.onResume(); launchView { load() } }

    /** Lee el carrito (R) y descarta productos agotados o despublicados. */
    private suspend fun load() {
        items = Session.repo.getCart(uid)
        adapter.submit(items)
        updateSummary()
    }

    /** Recalcula subtotal, total y la insignia del carrito. */
    private fun updateSummary() {
        if (_binding == null) return
        val count = items.sumOf { it.quantity }
        val subtotal = items.sumOf { it.subtotal }
        val empty = items.isEmpty()
        binding.title.text = if (empty) getString(R.string.cart_title) else "${getString(R.string.cart_title)} ($count)"
        binding.emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        binding.summary.visibility = if (empty) View.GONE else View.VISIBLE
        binding.clearButton.visibility = if (empty) View.GONE else View.VISIBLE
        binding.subtotalLabel.text = "Subtotal ($count productos)"
        binding.subtotal.text = subtotal.toCop()
        binding.total.text = subtotal.toCop()
        (activity as? BuyerActivity)?.setBadge(R.id.nav_cart, count)
    }

    /** Libera la referencia al binding para evitar fugas de memoria. */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
