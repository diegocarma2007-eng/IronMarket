package com.ironmarket.app.data

/**
 * Contrato de acceso a datos de IronMarket.
 *
 * La interfaz desacopla las pantallas de la fuente de datos. Hay dos implementaciones:
 * - [FirebaseRepository]: Firebase Authentication, Cloud Firestore y Firebase Storage (producción).
 * - [SqliteRepository]: base de datos SQLite local con SQLiteOpenHelper (modo local y pruebas).
 *
 * Todas las funciones son `suspend`: se ejecutan fuera del hilo principal y se llaman desde corrutinas.
 */
interface Repository {

    /** Nombre de la fuente de datos, para mostrarlo en la interfaz (p. ej. "Firebase"). */
    val sourceName: String

    // ---------------- Autenticación ----------------

    /** Crea la cuenta, guarda el perfil y deja la sesión iniciada. */
    suspend fun register(name: String, email: String, phone: String, password: String, role: Role): User

    /** Inicia sesión con correo y contraseña y devuelve el perfil del usuario. */
    suspend fun login(email: String, password: String): User

    /** Envía el enlace de recuperación de contraseña al correo. */
    suspend fun sendPasswordReset(email: String)

    /** Devuelve el usuario con sesión activa (sesión persistente) o null. */
    suspend fun restoreSession(): User?

    /** Cierra la sesión. */
    fun logout()

    // ---------------- Usuarios (CRUD) ----------------

    /** Lista todos los usuarios no eliminados. */
    suspend fun getUsers(): List<User>
    /** Obtiene un usuario por su id, o null si no existe. */
    suspend fun getUser(id: String): User?

    /** Crea un usuario desde el panel de administración con una contraseña inicial. */
    suspend fun createUser(name: String, email: String, phone: String, role: Role, password: String): User
    /** Actualiza nombre, celular, rol, estado y verificación de un usuario. */
    suspend fun updateUser(user: User)

    /** Borrado lógico: la cuenta queda inactiva y anonimizada (Ley 1581 de 2012). */
    suspend fun deleteUser(id: String)

    // ---------------- Productos (CRUD) ----------------

    /** Lista productos filtrando opcionalmente por estado y por vendedor. */
    suspend fun getProducts(status: ProductStatus? = null, sellerId: String? = null): List<Product>
    /** Obtiene un producto por su id, o null si no existe. */
    suspend fun getProduct(id: String): Product?

    /** Crea o actualiza un producto. Si [image] no es null, sube la imagen (JPEG comprimido). */
    suspend fun saveProduct(product: Product, image: ByteArray?): Product
    /** Elimina un producto y su foto. */
    suspend fun deleteProduct(id: String)

    /** Genera un id nuevo para un producto. */
    fun newProductId(): String

    // ---------------- Carrito (CRUD) ----------------

    /** Devuelve el carrito del usuario con productos vigentes y cantidades ajustadas al stock. */
    suspend fun getCart(userId: String): List<CartItem>

    /** Fija la cantidad de un producto en el carrito; 0 lo elimina. */
    suspend fun setCartQuantity(userId: String, productId: String, quantity: Int)
    /** Vacía el carrito del usuario. */
    suspend fun clearCart(userId: String)

    // ---------------- Órdenes ----------------

    /**
     * Registra la compra: crea una orden por vendedor, descuenta el stock y vacía el carrito.
     * @return las órdenes creadas.
     */
    suspend fun checkout(
        buyer: User, items: List<CartItem>, delivery: String, address: String,
        lat: Double?, lng: Double?, shipping: Int, paymentRef: String
    ): List<Order>

    /** Lista órdenes de un vendedor, de un comprador o todas (administrador). */
    suspend fun getOrders(sellerId: String? = null, buyerId: String? = null): List<Order>
    /** Cambia el estado de una orden (por despachar, en camino, entregado). */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus)

    // ---------------- Utilidades ----------------

    /** Carga productos de ejemplo cuando la base de datos está vacía (solo administrador). */
    suspend fun seedDemoProducts(seller: User): Int
}
