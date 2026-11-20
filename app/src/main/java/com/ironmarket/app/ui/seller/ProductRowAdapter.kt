package com.ironmarket.app.ui.seller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ironmarket.app.R
import com.ironmarket.app.data.Product
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.databinding.ItemProductRowBinding
import com.ironmarket.app.util.Images
import com.ironmarket.app.util.color
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

    /** Reemplaza los elementos de la lista y redibuja. */
    fun submit(list: List<Product>) { items = list; notifyDataSetChanged() }

    /** Crea la vista de una fila a partir de su layout. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemProductRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    /** Cantidad de elementos de la lista. */
    override fun getItemCount() = items.size

    /** Llena la fila con los datos del elemento en la posición indicada. */
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.itemView.context
        with(holder.b) {
            name.text = p.name
            thumb.clipToOutline = true
            Images.show(thumb, p, (16 * ctx.resources.displayMetrics.density).toInt())
            val lowStock = p.stock <= 3
            val stockText = if (lowStock) "Stock ${p.stock} (bajo)" else "Stock ${p.stock}"
            details.text = if (adminMode) "${p.sellerName} · ${p.category.label} · ${p.price.toCop()}"
                           else "${p.price.toCop()} · $stockText"
            details.setTextColor(ctx.color(if (!adminMode && lowStock) R.color.im_danger else R.color.im_text_muted))
            status.statusBadge(p.status)
            editButton.setOnClickListener { onEdit(p) }
            deleteButton.setOnClickListener { onDelete(p) }
            moderation.visibility = if (adminMode && p.status == ProductStatus.EN_REVISION) View.VISIBLE else View.GONE
            approveButton.setOnClickListener { onModerate(p, true) }
            rejectButton.setOnClickListener { onModerate(p, false) }
        }
    }
}
