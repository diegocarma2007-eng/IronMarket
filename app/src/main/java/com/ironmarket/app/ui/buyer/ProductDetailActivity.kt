package com.ironmarket.app.ui.buyer

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.ironmarket.app.R
import com.ironmarket.app.data.Product
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityProductDetailBinding
import com.ironmarket.app.ui.common.RoleActivity
import com.ironmarket.app.util.Images
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.launchSafe
import com.ironmarket.app.util.toCop

/** Detalle de producto con la información exigida por la Ley 1480 de 2011 y "Agregar al carrito" (C). */
class ProductDetailActivity : RoleActivity() {

    companion object { const val EXTRA_ID = "product_id" }

    private lateinit var binding: ActivityProductDetailBinding
    private var qty = 1

    /** Configura el detalle del producto: infla la vista y registra los eventos de los controles. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasSession) return
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }

        launchSafe {
            val product = Session.repo.getProduct(intent.getStringExtra(EXTRA_ID).orEmpty())
            if (product == null) { finish(); return@launchSafe }
            bind(product)
        }
    }

    /** Pinta los datos del producto y configura los controles de cantidad. */
    private fun bind(product: Product) = with(binding) {
        Images.show(image, product, (70 * resources.displayMetrics.density).toInt())
        name.text = product.name
        seller.text = "${product.sellerName} · ★ 4,8"
        price.text = product.price.toCop()
        description.text = product.description
        stock.text = when {
            product.stock <= 0 -> "Sin stock por ahora"
            product.invima != null -> "Stock: ${product.stock} · Registro ${product.invima}"
            else -> "Stock disponible: ${product.stock}"
        }
        if (product.invima != null) invimaBadge.badge("Registro INVIMA verificado", R.color.im_lime, R.color.im_ink)
        else invimaBadge.badge(product.category.label, R.color.im_surface_high, R.color.im_text)
        if (product.isNew) conditionBadge.badge("Nuevo", R.color.im_surface_high, R.color.im_text)
        else conditionBadge.badge("Usado", R.color.im_orange, R.color.im_ink)

        minusButton.setOnClickListener { if (qty > 1) { qty--; quantity.text = qty.toString() } }
        plusButton.setOnClickListener { if (qty < product.stock) { qty++; quantity.text = qty.toString() } }

        addButton.isEnabled = product.stock > 0
        addButton.setOnClickListener {
            launchSafe(addButton) {
                val uid = Session.currentUser!!.id
                val current = Session.repo.getCart(uid).firstOrNull { it.product.id == product.id }?.quantity ?: 0
                Session.repo.setCartQuantity(uid, product.id, (current + qty).coerceAtMost(product.stock))
                Snackbar.make(root, "Agregado al carrito", Snackbar.LENGTH_LONG)
                    .setAction("Ver carrito") { finish() }
                    .setAnchorView(addButton).show()
            }
        }
    }
}
