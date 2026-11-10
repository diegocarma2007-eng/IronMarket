package com.ironmarket.app.ui.seller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ironmarket.app.data.Order
import com.ironmarket.app.data.OrderStatus
import com.ironmarket.app.databinding.ItemOrderBinding
import com.ironmarket.app.util.orderBadge
import com.ironmarket.app.util.toCop

/** Pedidos recibidos por el vendedor, con la siguiente acción según el estado. */
class OrderAdapter(
    private var items: List<Order>,
    private val onAdvance: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.VH>() {

    class VH(val b: ItemOrderBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<Order>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val o = items[position]
        with(holder.b) {
            orderId.text = "Pedido #${o.id}"
            status.orderBadge(o.status)
            summary.text = "${o.buyerName} · ${o.summary}"
            delivery.text = "${o.delivery} · ${o.date}"
            total.text = o.total.toCop()
            when (o.status) {
                OrderStatus.NUEVO -> { actionButton.visibility = View.VISIBLE; actionButton.text = "Marcar despachado" }
                OrderStatus.EN_CAMINO -> { actionButton.visibility = View.VISIBLE; actionButton.text = "Confirmar entrega" }
                OrderStatus.ENTREGADO -> actionButton.visibility = View.GONE
            }
            actionButton.setOnClickListener { onAdvance(o) }
        }
    }
}
