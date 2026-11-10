package com.ironmarket.app.data

/** Roles del sistema (documento técnico, sección 2.4). */
enum class Role(val label: String) {
    COMPRADOR("Comprador"), VENDEDOR("Vendedor"), ADMIN("Administrador")
}

/** Estado de una publicación: el administrador la revisa antes de publicarla. */
enum class ProductStatus(val label: String) {
    PUBLICADO("Publicado"), EN_REVISION("En revisión"), RECHAZADO("Rechazado")
}

enum class Category(val label: String) {
    SUPLEMENTOS("Suplementos"), EQUIPO("Equipo"), ROPA("Ropa"), ACCESORIOS("Accesorios")
}

data class User(
    val id: Int,
    var name: String,
    var email: String,
    var phone: String,
    var role: Role,
    var active: Boolean = true,
    var verified: Boolean = true,
    val password: String = "123456"
)

data class Product(
    val id: Int,
    var name: String,
    var category: Category,
    var price: Int,
    var stock: Int,
    var isNew: Boolean,
    var description: String,
    var invima: String?,
    var sellerId: Int,
    var status: ProductStatus,
    var sold: Int = 0
)

data class CartItem(val product: Product, var quantity: Int) {
    val subtotal: Int get() = product.price * quantity
}

enum class OrderStatus(val label: String) {
    NUEVO("Por despachar"), EN_CAMINO("En camino"), ENTREGADO("Entregado")
}

data class Order(
    val id: String,
    val buyerName: String,
    val sellerId: Int,
    val summary: String,
    val total: Int,
    val delivery: String,
    var status: OrderStatus,
    val date: String,
    val category: Category
)
