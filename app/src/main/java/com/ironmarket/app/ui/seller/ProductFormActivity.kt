package com.ironmarket.app.ui.seller

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.ironmarket.app.R
import com.ironmarket.app.data.Category
import com.ironmarket.app.data.Product
import com.ironmarket.app.data.ProductStatus
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityProductFormBinding
import com.ironmarket.app.ui.common.RoleActivity
import com.ironmarket.app.util.Images
import com.ironmarket.app.util.icon
import com.ironmarket.app.util.launchSafe

/**
 * Crear (C) y editar (U) productos.
 * La foto se toma con la cámara o se elige de la galería, se comprime y se sube a Firebase Storage.
 * Los suplementos exigen registro sanitario INVIMA (Decreto 3249 de 2006).
 */
class ProductFormActivity : RoleActivity() {

    companion object { const val EXTRA_ID = "product_id" }

    private lateinit var binding: ActivityProductFormBinding
    private var editing: Product? = null
    private var photo: Bitmap? = null

    /** Cámara: abre la app de cámara del dispositivo y devuelve la foto. */
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp -> bmp?.let { setPhoto(it) } }

    /** Galería: selector de fotos del sistema (no requiere permisos de almacenamiento). */
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { Images.fromUri(this, it)?.let { bmp -> setPhoto(bmp) } }
    }

    /** Configura el formulario de producto: infla la vista y registra los eventos de los controles. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasSession) return
        binding = ActivityProductFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.closeButton.setOnClickListener { finish() }
        binding.header.formTitle.text = getString(R.string.new_product)

        // Lista desplegable de categorías
        binding.categoryInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, Category.entries.map { it.label }))
        binding.categoryInput.setOnItemClickListener { _, _, pos, _ -> onCategory(Category.entries[pos]) }

        binding.cameraButton.setOnClickListener {
            runCatching { takePicture.launch(null) }.onFailure { Toast.makeText(this, "No hay app de cámara disponible", Toast.LENGTH_SHORT).show() }
        }
        binding.galleryButton.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.saveButton.setOnClickListener { save() }
        binding.photo.setImageResource(R.drawable.ic_box)
        if (Session.role == Role.ADMIN) binding.reviewNote.text = "Como administrador, los cambios se publican de inmediato."

        // Modo edición: carga el producto desde la base de datos
        intent.getStringExtra(EXTRA_ID)?.let { id ->
            binding.header.formTitle.text = getString(R.string.edit_product)
            launchSafe {
                val p = Session.repo.getProduct(id) ?: return@launchSafe
                editing = p
                binding.nameInput.setText(p.name)
                binding.categoryInput.setText(p.category.label, false)
                binding.priceInput.setText(p.price.toString())
                binding.stockInput.setText(p.stock.toString())
                binding.descriptionInput.setText(p.description)
                binding.invimaInput.setText(p.invima)
                binding.conditionToggle.check(if (p.isNew) R.id.conditionNew else R.id.conditionUsed)
                onCategory(p.category)
                binding.photo.clipToOutline = true
                Images.show(binding.photo, p, (22 * resources.displayMetrics.density).toInt())
            }
        }
    }

    /** Muestra la foto elegida y la comprime para subirla. */
    private fun setPhoto(bmp: Bitmap) {
        photo = bmp
        binding.photo.imageTintList = null
        binding.photo.setPadding(0, 0, 0, 0)
        binding.photo.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.photo.clipToOutline = true
        binding.photo.setImageBitmap(bmp)
    }

    /** Muestra el campo INVIMA solo para suplementos. */
    private fun onCategory(cat: Category) {
        binding.invimaGroup.visibility = if (cat == Category.SUPLEMENTOS) View.VISIBLE else View.GONE
        if (photo == null && editing?.imageUrl == null) binding.photo.setImageResource(cat.icon())
    }

    /** Valida el formulario y guarda el producto (con su imagen) en la base de datos. */
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
        if (category == Category.SUPLEMENTOS && invima == null) { binding.invimaLayout.error = "Sin registro INVIMA no se puede publicar"; ok = false }
        if (!ok) return

        val user = Session.currentUser!!
        val isAdmin = user.role == Role.ADMIN
        val isNew = binding.conditionToggle.checkedButtonId == R.id.conditionNew
        val description = binding.descriptionInput.text.toString().trim()
        val product = editing?.apply {
            this.name = name; this.category = category!!; this.price = price!!; this.stock = stock!!
            this.isNew = isNew; this.description = description
            this.invima = if (category == Category.SUPLEMENTOS) invima else null
            if (!isAdmin) status = ProductStatus.EN_REVISION   // los cambios del vendedor vuelven a revisión
        } ?: Product(
            Session.repo.newProductId(), name, category!!, price!!, stock!!, isNew, description,
            if (category == Category.SUPLEMENTOS) invima else null, user.id, user.name,
            if (isAdmin) ProductStatus.PUBLICADO else ProductStatus.EN_REVISION
        )
        launchSafe(binding.saveButton) {
            binding.saveButton.text = "Guardando…"
            Session.repo.saveProduct(product, photo?.let { Images.compress(it) })
            Toast.makeText(this, if (isAdmin) "Producto guardado" else "Enviado a revisión", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
