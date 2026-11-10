package com.ironmarket.app.ui.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ironmarket.app.data.CartItem
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.databinding.ItemCartBinding
import com.ironmarket.app.util.icon
import com.ironmarket.app.util.toCop

/** Adaptador del carrito: cambiar cantidad (U) y eliminar (D). */
class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    class VH(val b: ItemCartBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.b) {
            name.text = item.product.name
            seller.text = DataRepository.sellerName(item.product.sellerId)
            quantity.text = item.quantity.toString()
            price.text = item.subtotal.toCop()
            thumb.setImageResource(item.product.category.icon())
            plusButton.setOnClickListener {
                if (item.quantity < item.product.stock) { item.quantity++; notifyItemChanged(holder.bindingAdapterPosition); onChanged() }
            }
            minusButton.setOnClickListener {
                if (item.quantity > 1) { item.quantity--; notifyItemChanged(holder.bindingAdapterPosition); onChanged() }
            }
            deleteButton.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                items.removeAt(pos); notifyItemRemoved(pos); onChanged()
            }
        }
    }
}
