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
import com.ironmarket.app.util.toDate

/** Pedidos recibidos por el vendedor, con la siguiente acción según el estado. */
class OrderAdapter(
    private var items: List<Order>,
    private val onAdvance: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.VH>() {

    class VH(val b: ItemOrderBinding) : RecyclerView.ViewHolder(b.root)

    /** Reemplaza los elementos de la lista y redibuja. */
    fun submit(list: List<Order>) { items = list; notifyDataSetChanged() }

    /** Crea la vista de una fila a partir de su layout. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    /** Cantidad de elementos de la lista. */
    override fun getItemCount() = items.size

    /** Llena la fila con los datos del elemento en la posición indicada. */
    override fun onBindViewHolder(holder: VH, position: Int) {
        val o = items[position]
        with(holder.b) {
            orderId.text = "Pedido #" + if (o.id.startsWith("IM-")) o.id.removePrefix("IM-") else o.id.takeLast(8).uppercase()
            status.orderBadge(o.status)
            summary.text = "${o.buyerName} · ${o.summary}"
            delivery.text = "${o.delivery} · ${o.createdAt.toDate()}\n${o.address} · Ref. ePayco ${o.paymentRef}"
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
