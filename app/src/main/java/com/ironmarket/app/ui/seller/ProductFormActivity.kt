package com.ironmarket.app.ui.seller

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.ironmarket.app.R
import com.ironmarket.app.data.Category
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.Product
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityProductFormBinding
import com.ironmarket.app.ui.common.RoleActivity
import com.ironmarket.app.util.icon

/**
 * Crear (C) y editar (U) productos. Usa la cámara del dispositivo para la foto
 * y exige registro INVIMA cuando la categoría es Suplementos.
 */
class ProductFormActivity : RoleActivity() {

    companion object { const val EXTRA_ID = "product_id" }

    private lateinit var binding: ActivityProductFormBinding
    private var editing: Product? = null
    private var photo: Bitmap? = null

    // Cámara: abre la app de cámara y devuelve una miniatura
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            photo = bmp
            binding.photo.imageTintList = null
            binding.photo.setPadding(0, 0, 0, 0)
            binding.photo.setImageBitmap(bmp)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editing = DataRepository.findProduct(intent.getIntExtra(EXTRA_ID, -1))
        binding.header.formTitle.text = getString(if (editing == null) R.string.new_product else R.string.edit_product)
        binding.header.closeButton.setOnClickListener { finish() }

        // Lista desplegable de categorías
        val labels = Category.entries.map { it.label }
        binding.categoryInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels))
        binding.categoryInput.setOnItemClickListener { _, _, pos, _ -> onCategory(Category.entries[pos]) }

        binding.cameraButton.setOnClickListener {
            try { takePicture.launch(null) }
            catch (e: Exception) { Toast.makeText(this, "No hay app de cámara disponible", Toast.LENGTH_SHORT).show() }
        }
        binding.saveButton.setOnClickListener { save() }

        editing?.let { p ->
            binding.nameInput.setText(p.name)
            binding.categoryInput.setText(p.category.label, false)
            binding.priceInput.setText(p.price.toString())
            binding.stockInput.setText(p.stock.toString())
            binding.descriptionInput.setText(p.description)
            binding.invimaInput.setText(p.invima)
            binding.conditionToggle.check(if (p.isNew) R.id.conditionNew else R.id.conditionUsed)
            onCategory(p.category)
        } ?: binding.photo.setImageResource(R.drawable.ic_box)

        if (Session.role == Role.ADMIN) binding.reviewNote.text = "Como administrador, los cambios se publican de inmediato."
    }

    private fun onCategory(cat: Category) {
        binding.invimaGroup.visibility = if (cat == Category.SUPLEMENTOS) View.VISIBLE else View.GONE
        if (photo == null) binding.photo.setImageResource(cat.icon())
    }

    private fun save() {
        listOf(binding.nameLayout, binding.categoryLayout, binding.priceLayout, binding.stockLayout, binding.invimaLayout).forEach { it.error = null }
        val name = binding.nameInput.text.toString().trim()
        val category = Category.entries.firstOrNull { it.label == binding.categoryInput.text.toString() }
        val price = binding.priceInput.text.toString().toIntOrNull()
        val stock = binding.stockInput.text.toString().toIntOrNull()
        val invima = binding.invimaInput.text.toString().trim().ifEmpty { null }

        var ok = true
        if (name.length < 3) { binding.nameLayout.error = getString(R.string.error_required); ok = false }
        if (category == null) { binding.categoryLayout.error = "Elige una categoría"; ok = false }
        if (price == null || price <= 0) { binding.priceLayout.error = "Precio mayor que 0"; ok = false }
        if (stock == null || stock < 0) { binding.stockLayout.error = "Stock válido"; ok = false }
        if (category == Category.SUPLEMENTOS && invima == null) {
            binding.invimaLayout.error = "Sin registro INVIMA no se puede publicar"; ok = false
        }
        if (!ok) return

        val isAdmin = Session.role == Role.ADMIN
        val product = editing?.apply {
            this.name = name; this.category = category!!; this.price = price!!; this.stock = stock!!
            this.isNew = binding.conditionToggle.checkedButtonId == R.id.conditionNew
            this.description = binding.descriptionInput.text.toString().trim()
            this.invima = if (category == Category.SUPLEMENTOS) invima else null
            if (!isAdmin) status = ProductStatus.EN_REVISION   // los cambios vuelven a revisión
        } ?: Product(
            DataRepository.newProductId(), name, category!!, price!!, stock!!,
            binding.conditionToggle.checkedButtonId == R.id.conditionNew,
            binding.descriptionInput.text.toString().trim(),
            if (category == Category.SUPLEMENTOS) invima else null,
            Session.currentUser?.id ?: 2,
            if (isAdmin) ProductStatus.PUBLICADO else ProductStatus.EN_REVISION
        )
        DataRepository.saveProduct(product)
        Toast.makeText(this, if (isAdmin) "Producto guardado" else "Enviado a revisión", Toast.LENGTH_SHORT).show()
        finish()
    }
}
