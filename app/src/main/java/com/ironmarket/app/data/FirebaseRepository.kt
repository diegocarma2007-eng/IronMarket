package com.ironmarket.app.data

import android.content.Context
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/** Tiempo máximo para subir o borrar una foto en Storage antes de usar el respaldo en Base64. */
private const val STORAGE_TIMEOUT_MS = 8_000L

/**
 * Implementación de [Repository] con Firebase.
 *
 * - **Firebase Authentication**: registro, inicio de sesión, recuperación de contraseña y sesión persistente.
 * - **Cloud Firestore**: colecciones `users`, `products`, `carts/{uid}/items` y `orders`.
 * - **Firebase Storage**: imágenes de productos en `products/{id}.jpg`. Si Storage no está habilitado
 *   (plan Spark), la imagen comprimida se guarda en Base64 dentro del documento como respaldo.
 *
 * Las reglas de seguridad que acompañan este código están en `firebase/firestore.rules` y `firebase/storage.rules`.
 */
class FirebaseRepository(private val context: Context) : Repository {

    override val sourceName = "Firebase"

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")
    private val products = db.collection("products")
    private val orders = db.collection("orders")

    // =====================================================================
    // Autenticación
    // =====================================================================

    /** Registra una cuenta nueva y su perfil con el rol elegido. */
    override suspend fun register(name: String, email: String, phone: String, password: String, role: Role): User = guard {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user!!.uid
        // Los correos configurados en ADMIN_EMAILS reciben el rol de administrador
        val finalRole = if (SqliteRepository.isAdminEmail(email)) Role.ADMIN else role
        val user = User(uid, name, email.trim().lowercase(), phone, finalRole, verified = finalRole != Role.VENDEDOR)
        users.document(uid).set(user.toMap() + ("createdAt" to FieldValue.serverTimestamp())).await()
        user
    }

    /** Inicia sesión con correo y contraseña; rechaza cuentas suspendidas o eliminadas. */
    override suspend fun login(email: String, password: String): User = guard {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val fbUser = result.user!!
        val user = getUser(fbUser.uid) ?: User(fbUser.uid, fbUser.email ?: email, email.trim().lowercase(), "",
            if (SqliteRepository.isAdminEmail(email)) Role.ADMIN else Role.COMPRADOR).also {
            // Cuenta creada desde la consola de Firebase sin perfil: se crea el perfil por defecto
            users.document(it.id).set(it.toMap()).await()
        }
        if (user.deleted || !user.active) {
            auth.signOut()
            throw AppException(if (user.deleted) "Esta cuenta fue eliminada" else "Tu cuenta está suspendida. Contacta a soporte.")
        }
        user
    }

    /** Envía el enlace para restablecer la contraseña al correo indicado. */
    override suspend fun sendPasswordReset(email: String) = guard {
        auth.setLanguageCode("es")
        auth.sendPasswordResetEmail(email.trim()).await(); Unit
    }

    /** Recupera el usuario de una sesión abierta anteriormente, o null. */
    override suspend fun restoreSession(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return runCatching { getUser(uid) }.getOrNull()?.takeIf { it.active && !it.deleted }
    }

    /** Cierra la sesión actual. */
    override fun logout() = auth.signOut()

    // =====================================================================
    // Usuarios
    // =====================================================================

    /** Lista todos los usuarios no eliminados. */
    override suspend fun getUsers(): List<User> = guard {
        users.get().await().documents.mapNotNull { it.toUser() }.filter { !it.deleted }.sortedBy { it.name }
    }

    /** Obtiene un usuario por su id, o null si no existe. */
    override suspend fun getUser(id: String): User? = guard { users.document(id).get().await().toUser() }

    /**
     * Crea una cuenta desde el panel de administración. Se usa una instancia secundaria de FirebaseApp
     * para que el administrador no pierda su propia sesión al crear la cuenta nueva.
     */
    override suspend fun createUser(name: String, email: String, phone: String, role: Role, password: String): User = guard {
        val primary = FirebaseApp.getInstance()
        val secondary = FirebaseApp.getApps(context).firstOrNull { it.name == "admin-create" }
            ?: FirebaseApp.initializeApp(context, primary.options, "admin-create")
        val secondaryAuth = FirebaseAuth.getInstance(secondary)
        val uid = secondaryAuth.createUserWithEmailAndPassword(email.trim(), password).await().user!!.uid
        secondaryAuth.signOut()
        val user = User(uid, name, email.trim().lowercase(), phone, role)
        users.document(uid).set(user.toMap() + ("createdAt" to FieldValue.serverTimestamp())).await()
        user
    }

    /** Actualiza nombre, celular, rol, estado y verificación de un usuario. */
    override suspend fun updateUser(user: User) = guard {
        users.document(user.id).set(user.toMap(), SetOptions.merge()).await(); Unit
    }

    /** Borrado lógico y anonimización: los pedidos y facturas se conservan por obligación legal. */
    override suspend fun deleteUser(id: String) = guard {
        users.document(id).update(mapOf("deleted" to true, "active" to false, "name" to "Usuario eliminado",
            "phone" to "", "address" to "")).await(); Unit
    }

    // =====================================================================
    // Productos
    // =====================================================================

    /** Lista productos filtrando opcionalmente por estado y vendedor. */
    override suspend fun getProducts(status: ProductStatus?, sellerId: String?): List<Product> = guard {
        var query: com.google.firebase.firestore.Query = products
        status?.let { query = query.whereEqualTo("status", it.name) }
        sellerId?.let { query = query.whereEqualTo("sellerId", it) }
        // Se ordena en el cliente para no requerir índices compuestos en Firestore
        query.get().await().documents.mapNotNull { it.toProduct() }.sortedByDescending { it.createdAt }
    }

    /** Obtiene un producto por su id, o null si no existe. */
    override suspend fun getProduct(id: String): Product? = guard { products.document(id).get().await().toProduct() }

    /** Crea o actualiza un producto y guarda su foto si se envía una nueva. */
    override suspend fun saveProduct(product: Product, image: ByteArray?): Product = guard {
        if (image != null) product.imageUrl = uploadImage(product.id, image)
        products.document(product.id).set(product.toMap()).await()
        product
    }

    /** Sube la imagen a Firebase Storage; si no está disponible, la guarda en Base64 (respaldo). */
    private suspend fun uploadImage(productId: String, image: ByteArray): String = try {
        // Sin plan Blaze no hay bucket: se limita la espera para no bloquear al vendedor
        val url = withTimeoutOrNull(STORAGE_TIMEOUT_MS) {
            val ref = storage().reference.child("products/$productId.jpg")
            ref.putBytes(image).await()
            ref.downloadUrl.await().toString()
        }
        url ?: base64(image)
    } catch (e: Exception) {
        base64(image)
    }

    /** Instancia de Storage con reintentos cortos (por defecto el SDK reintenta durante minutos). */
    private fun storage(): FirebaseStorage = FirebaseStorage.getInstance().apply {
        maxUploadRetryTimeMillis = STORAGE_TIMEOUT_MS
        maxOperationRetryTimeMillis = STORAGE_TIMEOUT_MS
    }

    /** Imagen comprimida como data URI para guardarla dentro del documento del producto. */
    private fun base64(image: ByteArray) = "data:image/jpeg;base64," + Base64.encodeToString(image, Base64.NO_WRAP)

    /** Elimina un producto y su foto. */
    override suspend fun deleteProduct(id: String) = guard {
        products.document(id).delete().await()
        runCatching { withTimeoutOrNull(STORAGE_TIMEOUT_MS) { storage().reference.child("products/$id.jpg").delete().await() } }
        Unit
    }

    /** Genera un identificador nuevo para un producto. */
    override fun newProductId(): String = products.document().id

    // =====================================================================
    // Carrito: carts/{uid}/items/{productId} = { productId, quantity }
    // =====================================================================

    /** Referencia a la subcolección de ítems del carrito del usuario. */
    private fun cartItems(uid: String) = db.collection("carts").document(uid).collection("items")

    /** Devuelve el carrito del usuario con productos vigentes y cantidades ajustadas al stock. */
    override suspend fun getCart(userId: String): List<CartItem> = guard {
        cartItems(userId).get().await().documents.mapNotNull { doc ->
            val qty = (doc.getLong("quantity") ?: 0L).toInt()
            // Si el producto fue despublicado o eliminado, simplemente no aparece en el carrito
            runCatching { getProduct(doc.id) }.getOrNull()?.takeIf { it.stock > 0 }?.let { CartItem(it, qty.coerceAtMost(it.stock)) }
        }
    }

    /** Fija la cantidad de un producto en el carrito; 0 lo elimina. */
    override suspend fun setCartQuantity(userId: String, productId: String, quantity: Int) = guard {
        val doc = cartItems(userId).document(productId)
        if (quantity <= 0) doc.delete().await()
        else doc.set(mapOf("productId" to productId, "quantity" to quantity, "updatedAt" to FieldValue.serverTimestamp())).await()
        Unit
    }

    /** Vacía el carrito del usuario. */
    override suspend fun clearCart(userId: String) = guard {
        val batch = db.batch()
        cartItems(userId).get().await().documents.forEach { batch.delete(it.reference) }
        batch.commit().await(); Unit
    }

    // =====================================================================
    // Órdenes
    // =====================================================================

    /** Registra la compra en un lote atómico: órdenes, descuento de stock y vaciado del carrito. */
    override suspend fun checkout(buyer: User, items: List<CartItem>, delivery: String, address: String,
                                  lat: Double?, lng: Double?, shipping: Int, paymentRef: String): List<Order> = guard {
        val batch = db.batch()
        val created = items.groupBy { it.product.sellerId }.map { (sellerId, group) ->
            val ref = orders.document()
            val order = Order(ref.id, buyer.id, buyer.name, sellerId, group.joinToString { "${it.product.name} ×${it.quantity}" },
                group.sumOf { it.subtotal } + shipping, delivery, address, lat, lng, OrderStatus.NUEVO, paymentRef,
                System.currentTimeMillis(), group.first().product.category)
            batch.set(ref, order.toMap())
            group.forEach {
                batch.update(products.document(it.product.id), mapOf(
                    "stock" to FieldValue.increment(-it.quantity.toLong()),
                    "sold" to FieldValue.increment(it.quantity.toLong())))
                batch.delete(cartItems(buyer.id).document(it.product.id))
            }
            order
        }
        batch.commit().await()
        created
    }

    /** Lista órdenes filtrando opcionalmente por vendedor o comprador, de la más reciente a la más antigua. */
    override suspend fun getOrders(sellerId: String?, buyerId: String?): List<Order> = guard {
        val query = when {
            sellerId != null -> orders.whereEqualTo("sellerId", sellerId)
            buyerId != null -> orders.whereEqualTo("buyerId", buyerId)
            else -> orders
        }
        query.get().await().documents.mapNotNull { it.toOrder() }.sortedByDescending { it.createdAt }
    }

    /** Cambia el estado de una orden (por despachar, en camino, entregado). */
    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus) = guard {
        orders.document(orderId).update("status", status.name).await(); Unit
    }

    /** Carga productos de ejemplo a nombre del vendedor indicado. */
    override suspend fun seedDemoProducts(seller: User): Int = guard {
        val batch = db.batch()
        val list = DemoData.buildProducts(seller.id, seller.name) { products.document().id }
        list.forEach { batch.set(products.document(it.id), it.toMap()) }
        batch.commit().await()
        list.size
    }

    // =====================================================================
    // Conversión entre documentos de Firestore y modelos
    // =====================================================================

    /** Ejecuta una operación y traduce los errores de Firebase a mensajes en español. */
    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: AppException) {
        throw e
    } catch (e: FirebaseAuthException) {
        throw AppException(when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "El correo no es válido"
            "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" -> "Correo o contraseña incorrectos"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado"
            "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener mínimo 6 caracteres"
            "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera unos minutos."
            else -> "Error de autenticación: ${e.localizedMessage}"
        })
    } catch (e: Exception) {
        throw AppException("No se pudo conectar con el servidor. Revisa tu conexión. (${e.localizedMessage})")
    }

    /** Convierte el modelo en un mapa para guardarlo en la base de datos. */
    private fun User.toMap() = mapOf("name" to name, "email" to email, "phone" to phone, "role" to role.name,
        "active" to active, "verified" to verified, "address" to address, "deleted" to deleted)

    /** Convierte el registro leído en un objeto User. */
    private fun DocumentSnapshot.toUser(): User? {
        if (!exists()) return null
        return User(id, getString("name") ?: "", getString("email") ?: "", getString("phone") ?: "",
            runCatching { Role.valueOf(getString("role") ?: "") }.getOrDefault(Role.COMPRADOR),
            getBoolean("active") ?: true, getBoolean("verified") ?: true, getString("address") ?: "",
            getBoolean("deleted") ?: false)
    }

    /** Convierte el modelo en un mapa para guardarlo en la base de datos. */
    private fun Product.toMap() = mapOf("name" to name, "category" to category.name, "price" to price, "stock" to stock,
        "isNew" to isNew, "description" to description, "invima" to invima, "sellerId" to sellerId,
        "sellerName" to sellerName, "status" to status.name, "sold" to sold, "imageUrl" to imageUrl, "createdAt" to createdAt)

    /** Convierte el registro leído en un objeto Product. */
    private fun DocumentSnapshot.toProduct(): Product? {
        if (!exists()) return null
        return Product(id, getString("name") ?: "", runCatching { Category.valueOf(getString("category")!!) }.getOrDefault(Category.ACCESORIOS),
            (getLong("price") ?: 0).toInt(), (getLong("stock") ?: 0).toInt(), getBoolean("isNew") ?: true,
            getString("description") ?: "", getString("invima"), getString("sellerId") ?: "", getString("sellerName") ?: "Vendedor",
            runCatching { ProductStatus.valueOf(getString("status")!!) }.getOrDefault(ProductStatus.EN_REVISION),
            (getLong("sold") ?: 0).toInt(), getString("imageUrl"), getLong("createdAt") ?: 0L)
    }

    /** Convierte el modelo en un mapa para guardarlo en la base de datos. */
    private fun Order.toMap() = mapOf("buyerId" to buyerId, "buyerName" to buyerName, "sellerId" to sellerId,
        "summary" to summary, "total" to total, "delivery" to delivery, "address" to address, "lat" to lat, "lng" to lng,
        "status" to status.name, "paymentRef" to paymentRef, "createdAt" to createdAt, "category" to category.name)

    /** Convierte el registro leído en un objeto Order. */
    private fun DocumentSnapshot.toOrder(): Order? {
        if (!exists()) return null
        return Order(id, getString("buyerId") ?: "", getString("buyerName") ?: "", getString("sellerId") ?: "",
            getString("summary") ?: "", (getLong("total") ?: 0).toInt(), getString("delivery") ?: "",
            getString("address") ?: "", getDouble("lat"), getDouble("lng"),
            runCatching { OrderStatus.valueOf(getString("status")!!) }.getOrDefault(OrderStatus.NUEVO),
            getString("paymentRef") ?: "", getLong("createdAt") ?: 0L,
            runCatching { Category.valueOf(getString("category")!!) }.getOrDefault(Category.ACCESORIOS))
    }
}
