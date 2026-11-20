package com.ironmarket.app.data

/** Roles del sistema (documento técnico, sección 2.4). Determinan el panel y los permisos. */
enum class Role(val label: String) {
    COMPRADOR("Comprador"), VENDEDOR("Vendedor"), ADMIN("Administrador")
}

/** Estado de una publicación: el administrador la revisa antes de mostrarla en el catálogo. */
enum class ProductStatus(val label: String) {
    PUBLICADO("Publicado"), EN_REVISION("En revisión"), RECHAZADO("Rechazado")
}

/** Categorías del catálogo fitness. */
enum class Category(val label: String) {
    SUPLEMENTOS("Suplementos"), EQUIPO("Equipo"), ROPA("Ropa"), ACCESORIOS("Accesorios")
}

/** Estados del ciclo de vida de una orden. */
enum class OrderStatus(val label: String) {
    NUEVO("Por despachar"), EN_CAMINO("En camino"), ENTREGADO("Entregado")
}

/**
 * Usuario de la plataforma. Se guarda en la colección `users/{id}` de Firestore
 * (o en la tabla `users` de SQLite en modo local).
 * El borrado es lógico: [deleted] marca la cuenta como eliminada y anonimizada.
 */
data class User(
    val id: String,
    var name: String,
    var email: String,
    var phone: String,
    var role: Role,
    var active: Boolean = true,
    var verified: Boolean = true,
    var address: String = "",
    var deleted: Boolean = false
)

/**
 * Producto publicado por un vendedor. Colección `products/{id}`.
 * [imageUrl] puede ser una URL de Firebase Storage, una imagen en Base64 (`data:image/jpeg;base64,...`)
 * o una ruta local en modo SQLite. [sellerName] se guarda desnormalizado para no consultar al vendedor.
 */
data class Product(
    val id: String,
    var name: String,
    var category: Category,
    var price: Int,
    var stock: Int,
    var isNew: Boolean,
    var description: String,
    var invima: String?,
    var sellerId: String,
    var sellerName: String,
    var status: ProductStatus,
    var sold: Int = 0,
    var imageUrl: String? = null,
    var createdAt: Long = System.currentTimeMillis()
)

/** Ítem del carrito. Colección `carts/{uid}/items/{productId}`. */
data class CartItem(val product: Product, var quantity: Int) {
    val subtotal: Int get() = product.price * quantity
}

/**
 * Orden de compra. Colección `orders/{id}`. Se crea una orden por cada vendedor del carrito,
 * con la referencia de pago de ePayco ([paymentRef]) y la ubicación de entrega ([lat], [lng]).
 */
data class Order(
    val id: String,
    val buyerId: String,
    val buyerName: String,
    val sellerId: String,
    val summary: String,
    val total: Int,
    val delivery: String,
    val address: String,
    val lat: Double? = null,
    val lng: Double? = null,
    var status: OrderStatus,
    val paymentRef: String,
    val createdAt: Long,
    val category: Category
)

/** Error de negocio con un mensaje listo para mostrar al usuario. */
class AppException(message: String) : Exception(message)
