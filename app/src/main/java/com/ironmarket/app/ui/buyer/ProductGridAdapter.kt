package com.ironmarket.app.ui.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.Product
import com.ironmarket.app.databinding.ItemProductGridBinding
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.icon
import com.ironmarket.app.util.toCop

/** Adaptador de tarjetas de producto (catálogo y más vendidos). */
class ProductGridAdapter(
    private var items: List<Product>,
    private val fixedWidthPx: Int? = null,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductGridAdapter.VH>() {

    class VH(val b: ItemProductGridBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<Product>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemProductGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        fixedWidthPx?.let { b.root.layoutParams.width = it }
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        with(holder.b) {
            name.text = p.name
            seller.text = DataRepository.sellerName(p.sellerId)
            price.text = p.price.toCop()
            thumb.setImageResource(p.category.icon())
            when {
                p.stock == 0 -> badge.badge("AGOTADO", R.color.im_surface_high, R.color.im_text_muted)
                p.invima != null -> badge.badge("INVIMA ✓", R.color.im_lime, R.color.im_ink)
                !p.isNew -> badge.badge("USADO", R.color.im_orange, R.color.im_ink)
                else -> badge.badge("NUEVO", R.color.im_surface_high, R.color.im_text)
            }
            card.setOnClickListener { onClick(p) }
        }
    }
}
