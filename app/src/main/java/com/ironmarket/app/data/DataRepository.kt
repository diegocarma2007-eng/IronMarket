package com.ironmarket.app.data

/**
 * Repositorio en memoria con datos de ejemplo.
 * Esta entrega corresponde al frontend; en la siguiente fase estas funciones
 * se conectarán a Firebase (Firestore y Storage) según la arquitectura del documento técnico.
 */
object DataRepository {

    val users = mutableListOf(
        User(1, "Diego Jiménez", "comprador@ironmarket.co", "300 000 0001", Role.COMPRADOR),
        User(2, "Suplementos La Fuerza", "vendedor@ironmarket.co", "300 000 0002", Role.VENDEDOR),
        User(3, "Ana Morales", "admin@ironmarket.co", "300 000 0003", Role.ADMIN),
        User(4, "Hierro de Segunda S.A.S.", "hierro@ironmarket.co", "300 000 0004", Role.VENDEDOR, verified = false),
        User(5, "Laura Martínez", "laura@correo.com", "300 000 0005", Role.COMPRADOR),
        User(6, "Carlos Ramírez", "carlos@correo.com", "300 000 0006", Role.COMPRADOR, active = false)
    )

    val products = mutableListOf(
        Product(1, "Whey Protein Isolate 2 lb", Category.SUPLEMENTOS, 189_900, 18, true,
            "Proteína aislada de suero con 27 g de proteína por servicio. Sabor vainilla.", "RSA-0012345-2024", 2, ProductStatus.PUBLICADO, 312),
        Product(2, "Creatina monohidratada 300 g", Category.SUPLEMENTOS, 109_900, 2, true,
            "Creatina micronizada, 60 servicios de 5 g. Sin sabor.", "RSA-0067890-2025", 2, ProductStatus.PUBLICADO, 205),
        Product(3, "Par mancuernas hexagonales 10 kg", Category.EQUIPO, 150_000, 1, false,
            "Mancuernas de caucho en buen estado, poco uso. Recogida en tienda.", null, 4, ProductStatus.PUBLICADO, 41),
        Product(4, "Cinturón de levantamiento en cuero", Category.ACCESORIOS, 95_000, 6, true,
            "Cinturón de 10 cm de ancho con hebilla doble. Tallas S a XL.", null, 2, ProductStatus.EN_REVISION),
        Product(5, "Guantes de entrenamiento talla M", Category.ACCESORIOS, 59_900, 0, true,
            "Guantes con muñequera y palma acolchada.", null, 2, ProductStatus.PUBLICADO, 88),
        Product(6, "Camiseta dry-fit hombre", Category.ROPA, 69_900, 25, true,
            "Tela transpirable de secado rápido. Colores negro y gris.", null, 2, ProductStatus.PUBLICADO, 120),
        Product(7, "Pre-entreno 30 servicios", Category.SUPLEMENTOS, 99_000, 10, true,
            "Fórmula con cafeína y beta-alanina.", "RSA-0055555-2025", 4, ProductStatus.EN_REVISION),
        Product(8, "Banco plano ajustable (usado)", Category.EQUIPO, 320_000, 1, false,
            "Banco con 6 posiciones, soporta 250 kg. Detalles mínimos de pintura.", null, 4, ProductStatus.PUBLICADO, 9),
        Product(9, "Leggings deportivos mujer", Category.ROPA, 79_900, 14, true,
            "Tiro alto, tela compresiva y bolsillo lateral.", null, 2, ProductStatus.PUBLICADO, 97)
    )

    val orders = mutableListOf(
        Order("IM-10482", "Diego J.", 2, "Whey Protein Isolate 2 lb ×1, Creatina 300 g ×1", 307_800, "Envío a domicilio", OrderStatus.NUEVO, "08/11/2026", Category.SUPLEMENTOS),
        Order("IM-10479", "Laura M.", 2, "Leggings deportivos mujer ×2", 159_800, "Recoge en tienda", OrderStatus.NUEVO, "08/11/2026", Category.ROPA),
        Order("IM-10466", "Andrés P.", 2, "Guantes de entrenamiento ×1", 67_900, "Envío a domicilio", OrderStatus.EN_CAMINO, "06/11/2026", Category.ACCESORIOS),
        Order("IM-10431", "Diego J.", 2, "Camiseta dry-fit ×3", 217_700, "Envío a domicilio", OrderStatus.ENTREGADO, "01/11/2026", Category.ROPA),
        Order("IM-10402", "Laura M.", 4, "Banco plano ajustable ×1", 320_000, "Recoge en tienda", OrderStatus.ENTREGADO, "28/10/2026", Category.EQUIPO)
    )

    val cart = mutableListOf<CartItem>()

    private var nextUserId = 100
    private var nextProductId = 100
    private var nextOrder = 10500

    // ---------- Usuarios (CRUD) ----------
    fun findUserByEmail(email: String) = users.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
    fun findUser(id: Int) = users.firstOrNull { it.id == id }

    fun createUser(name: String, email: String, phone: String, role: Role): User {
        val user = User(nextUserId++, name, email, phone, role, verified = role != Role.VENDEDOR)
        users.add(user)
        return user
    }

    fun updateUser(user: User) {
        val i = users.indexOfFirst { it.id == user.id }
        if (i >= 0) users[i] = user
    }

    fun deleteUser(id: Int) = users.removeAll { it.id == id }

    // ---------- Productos (CRUD) ----------
    fun findProduct(id: Int) = products.firstOrNull { it.id == id }
    fun publishedProducts() = products.filter { it.status == ProductStatus.PUBLICADO }
    fun productsOfSeller(sellerId: Int) = products.filter { it.sellerId == sellerId }

    fun saveProduct(product: Product) {
        val i = products.indexOfFirst { it.id == product.id }
        if (i >= 0) products[i] = product else products.add(product)
    }

    fun newProductId() = nextProductId++
    fun deleteProduct(id: Int) {
        products.removeAll { it.id == id }
        cart.removeAll { it.product.id == id }
    }

    fun sellerName(sellerId: Int) = findUser(sellerId)?.name ?: "Vendedor"

    // ---------- Carrito (CRUD) ----------
    fun addToCart(product: Product, quantity: Int) {
        val item = cart.firstOrNull { it.product.id == product.id }
        if (item != null) item.quantity = (item.quantity + quantity).coerceAtMost(product.stock)
        else cart.add(CartItem(product, quantity))
    }

    fun cartCount() = cart.sumOf { it.quantity }
    fun cartSubtotal() = cart.sumOf { it.subtotal }

    // ---------- Pedidos ----------
    fun ordersOfSeller(sellerId: Int) = orders.filter { it.sellerId == sellerId }

    /** Crea un pedido por cada vendedor del carrito y vacía el carrito. */
    fun checkout(buyer: User, delivery: String, shipping: Int): Int {
        var total = 0
        cart.groupBy { it.product.sellerId }.forEach { (sellerId, items) ->
            val subtotal = items.sumOf { it.subtotal } + shipping
            total += subtotal
            orders.add(0, Order(
                "IM-${nextOrder++}", buyer.name, sellerId,
                items.joinToString { "${it.product.name} ×${it.quantity}" },
                subtotal, delivery, OrderStatus.NUEVO, "09/11/2026", items.first().product.category
            ))
            items.forEach { it.product.stock -= it.quantity; it.product.sold += it.quantity }
        }
        cart.clear()
        return total
    }

    fun totalSales() = orders.sumOf { it.total }
}
