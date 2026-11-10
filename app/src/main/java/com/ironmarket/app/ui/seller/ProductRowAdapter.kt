package com.ironmarket.app.ui.seller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.Product
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.databinding.ItemProductRowBinding
import com.ironmarket.app.util.color
import com.ironmarket.app.util.icon
import com.ironmarket.app.util.statusBadge
import com.ironmarket.app.util.toCop

/**
 * Fila de producto con acciones. La usan el vendedor (editar/eliminar)
 * y el administrador (además aprobar/rechazar publicaciones en revisión).
 */
class ProductRowAdapter(
    private var items: List<Product>,
    private val adminMode: Boolean,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
    private val onModerate: (Product, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<ProductRowAdapter.VH>() {

    class VH(val b: ItemProductRowBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<Product>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemProductRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.itemView.context
        with(holder.b) {
            name.text = p.name
            thumb.setImageResource(p.category.icon())
            val stockText = if (p.stock <= 3) "Stock ${p.stock} (bajo)" else "Stock ${p.stock}"
            details.text = if (adminMode) "${DataRepository.sellerName(p.sellerId)} · ${p.category.label} · ${p.price.toCop()}"
                           else "${p.price.toCop()} · $stockText"
            details.setTextColor(ctx.color(if (!adminMode && p.stock <= 3) R.color.im_danger else R.color.im_text_muted))
            status.statusBadge(p.status)
            editButton.setOnClickListener { onEdit(p) }
            deleteButton.setOnClickListener { onDelete(p) }
            moderation.visibility = if (adminMode && p.status == ProductStatus.EN_REVISION) View.VISIBLE else View.GONE
            approveButton.setOnClickListener { onModerate(p, true) }
            rejectButton.setOnClickListener { onModerate(p, false) }
        }
    }
}
