package com.ironmarket.app.ui.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ironmarket.app.R
import com.ironmarket.app.data.Product
import com.ironmarket.app.databinding.ItemProductGridBinding
import com.ironmarket.app.util.Images
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.toCop

/** Adaptador de tarjetas de producto para el catálogo y la lista de más vendidos. */
class ProductGridAdapter(
    private var items: List<Product>,
    private val fixedWidthPx: Int? = null,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductGridAdapter.VH>() {

    class VH(val b: ItemProductGridBinding) : RecyclerView.ViewHolder(b.root)

    /** Reemplaza la lista mostrada. */
    fun submit(list: List<Product>) { items = list; notifyDataSetChanged() }

    /** Crea la vista de una fila a partir de su layout. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemProductGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        fixedWidthPx?.let { b.root.layoutParams.width = it }
        return VH(b)
    }

    /** Cantidad de elementos de la lista. */
    override fun getItemCount() = items.size

    /** Llena la fila con los datos del elemento en la posición indicada. */
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        with(holder.b) {
            name.text = p.name
            seller.text = p.sellerName
            price.text = p.price.toCop()
            Images.show(thumb, p, root.resources.getDimensionPixelSize(R.dimen.space_xxl) + 4)
            // Badge según el estado: agotado, registro sanitario, usado o nuevo
            when {
                p.stock <= 0 -> badge.badge("AGOTADO", R.color.im_surface_high, R.color.im_text_muted)
                p.invima != null -> badge.badge("INVIMA ✓", R.color.im_lime, R.color.im_ink)
                !p.isNew -> badge.badge("USADO", R.color.im_orange, R.color.im_ink)
                else -> badge.badge("NUEVO", R.color.im_surface_high, R.color.im_text)
            }
            card.setOnClickListener { onClick(p) }
        }
    }
}
