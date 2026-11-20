package com.ironmarket.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import coil.load
import com.ironmarket.app.data.Product
import java.io.ByteArrayOutputStream
import java.io.File

/** Utilidades para cargar y comprimir las imágenes de los productos. */
object Images {

    /**
     * Muestra la imagen del producto en [view]. Acepta URL de Firebase Storage, Base64 o ruta local.
     * Si el producto no tiene imagen, muestra el icono de su categoría coloreado con el acento del rol.
     */
    fun show(view: ImageView, product: Product, iconPaddingPx: Int) {
        val url = product.imageUrl
        if (url.isNullOrEmpty()) {
            showIcon(view, product, iconPaddingPx); return
        }
        view.imageTintList = null
        view.setPadding(0, 0, 0, 0)
        view.scaleType = ImageView.ScaleType.CENTER_CROP
        when {
            url.startsWith("data:") -> {
                val bytes = Base64.decode(url.substringAfter(","), Base64.DEFAULT)
                view.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }
            url.startsWith("/") -> view.load(File(url)) { crossfade(true) }
            else -> view.load(url) { crossfade(true); error(product.category.icon()) }
        }
    }

    /** Muestra el icono de la categoría cuando el producto no tiene foto. */
    private fun showIcon(view: ImageView, product: Product, paddingPx: Int) {
        val tint = view.context.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorPrimary))
        view.imageTintList = tint.getColorStateList(0); tint.recycle()
        view.scaleType = ImageView.ScaleType.FIT_CENTER
        view.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        view.setImageResource(product.category.icon())
    }

    /** Reduce la imagen a máximo 800 px y la comprime en JPEG (calidad 75) para ahorrar datos (RNF-04). */
    fun compress(bitmap: Bitmap): ByteArray {
        val scale = 800f / maxOf(bitmap.width, bitmap.height)
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
        return ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 75, it) }.toByteArray()
    }

    /** Lee una imagen elegida de la galería y la devuelve como Bitmap. */
    fun fromUri(context: Context, uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
}
