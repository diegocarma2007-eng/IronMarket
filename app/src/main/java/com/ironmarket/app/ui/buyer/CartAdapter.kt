package com.ironmarket.app.ui.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ironmarket.app.data.CartItem
import com.ironmarket.app.databinding.ItemCartBinding
import com.ironmarket.app.util.Images
import com.ironmarket.app.util.toCop

/**
 * Adaptador del carrito. Los cambios de cantidad (U) y eliminación (D) se notifican
 * con [onQuantity]; la pantalla los guarda en la base de datos.
 */
class CartAdapter(
    private var items: List<CartItem>,
    private val onQuantity: (CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    class VH(val b: ItemCartBinding) : RecyclerView.ViewHolder(b.root)

    /** Reemplaza los elementos de la lista y redibuja. */
    fun submit(list: List<CartItem>) { items = list; notifyDataSetChanged() }

    /** Crea la vista de una fila a partir de su layout. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    /** Cantidad de elementos de la lista. */
    override fun getItemCount() = items.size

    /** Llena la fila con los datos del elemento en la posición indicada. */
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.b) {
            name.text = item.product.name
            seller.text = item.product.sellerName
            quantity.text = item.quantity.toString()
            price.text = item.subtotal.toCop()
            thumb.clipToOutline = true
            Images.show(thumb, item.product, 16 * root.resources.displayMetrics.density.toInt())
            plusButton.setOnClickListener { if (item.quantity < item.product.stock) onQuantity(item, item.quantity + 1) }
            minusButton.setOnClickListener { if (item.quantity > 1) onQuantity(item, item.quantity - 1) }
            deleteButton.setOnClickListener { onQuantity(item, 0) }
        }
    }
}
