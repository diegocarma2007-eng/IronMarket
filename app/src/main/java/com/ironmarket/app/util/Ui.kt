package com.ironmarket.app.util

import android.content.Context
import android.util.Patterns
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ironmarket.app.R
import com.ironmarket.app.data.Category
import com.ironmarket.app.data.OrderStatus
import com.ironmarket.app.data.ProductStatus
import java.text.NumberFormat
import java.util.Locale

/** Formato de pesos colombianos: 189900 -> "$189.900" */
fun Int.toCop(): String = "$" + NumberFormat.getIntegerInstance(Locale("es", "CO")).format(this)

fun String.isValidEmail() = Patterns.EMAIL_ADDRESS.matcher(trim()).matches()

/** Iniciales para los avatares: "Diego Jiménez" -> "DJ" */
fun String.initials() = split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** Icono de cada categoría (se usa como miniatura del producto). */
fun Category.icon() = when (this) {
    Category.SUPLEMENTOS -> R.drawable.ic_box
    Category.EQUIPO -> R.drawable.ic_dumbbell
    Category.ROPA -> R.drawable.ic_user
    Category.ACCESORIOS -> R.drawable.ic_shield
}

/** Pinta un badge con texto y colores de fondo/texto. */
fun TextView.badge(text: String, bgColor: Int, textColor: Int) {
    this.text = text
    backgroundTintList = ContextCompat.getColorStateList(context, bgColor)
    setTextColor(ContextCompat.getColor(context, textColor))
}

fun TextView.statusBadge(status: ProductStatus) = when (status) {
    ProductStatus.PUBLICADO -> badge(status.label, R.color.im_lime_dark, R.color.im_lime)
    ProductStatus.EN_REVISION -> badge(status.label, R.color.im_warning_dark, R.color.im_warning)
    ProductStatus.RECHAZADO -> badge(status.label, R.color.im_danger_dark, R.color.im_danger)
}

fun TextView.orderBadge(status: OrderStatus) = when (status) {
    OrderStatus.NUEVO -> badge(status.label, R.color.im_orange_dark, R.color.im_orange)
    OrderStatus.EN_CAMINO -> badge(status.label, R.color.im_blue_dark, R.color.im_blue)
    OrderStatus.ENTREGADO -> badge(status.label, R.color.im_lime_dark, R.color.im_lime)
}

fun Context.color(res: Int) = ContextCompat.getColor(this, res)
