package com.ironmarket.app.ui.buyer

import android.os.Bundle
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.databinding.ActivityProductDetailBinding
import com.ironmarket.app.ui.common.RoleActivity
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.icon
import com.ironmarket.app.util.toCop

/** Detalle de producto: información exigida por la Ley 1480 de 2011 y agregar al carrito (C). */
class ProductDetailActivity : RoleActivity() {

    companion object { const val EXTRA_ID = "product_id" }

    private lateinit var binding: ActivityProductDetailBinding
    private var qty = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val product = DataRepository.findProduct(intent.getIntExtra(EXTRA_ID, -1))
        if (product == null) { finish(); return }

        with(binding) {
            backButton.setOnClickListener { finish() }
            image.setImageResource(product.category.icon())
            name.text = product.name
            seller.text = "${DataRepository.sellerName(product.sellerId)} · ★ 4,8 · 2,3 km"
            price.text = product.price.toCop()
            description.text = product.description
            stock.text = when {
                product.stock == 0 -> "Sin stock por ahora"
                product.invima != null -> "Stock: ${product.stock} · Registro ${product.invima}"
                else -> "Stock disponible: ${product.stock}"
            }
            if (product.invima != null) invimaBadge.badge("Registro INVIMA verificado", R.color.im_lime, R.color.im_ink)
            else invimaBadge.badge(product.category.label, R.color.im_surface_high, R.color.im_text)
            if (product.isNew) conditionBadge.badge("Nuevo", R.color.im_surface_high, R.color.im_text)
            else conditionBadge.badge("Usado", R.color.im_orange, R.color.im_ink)

            minusButton.setOnClickListener { if (qty > 1) { qty--; quantityLabel() } }
            plusButton.setOnClickListener { if (qty < product.stock) { qty++; quantityLabel() } }

            addButton.isEnabled = product.stock > 0
            addButton.setOnClickListener {
                DataRepository.addToCart(product, qty)
                Snackbar.make(root, "Agregado al carrito", Snackbar.LENGTH_LONG)
                    .setAction("Ver carrito") { finish() }
                    .setAnchorView(addButton).show()
            }
        }
        if (product.stock == 0) Toast.makeText(this, "Producto agotado", Toast.LENGTH_SHORT).show()
    }

    private fun quantityLabel() { binding.quantity.text = qty.toString() }
}
