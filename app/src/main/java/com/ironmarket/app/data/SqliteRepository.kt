package com.ironmarket.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ironmarket.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Implementación local de [Repository] con SQLite (SQLiteOpenHelper + SQLiteDatabase).
 *
 * Se usa cuando la app se compila sin `google-services.json` y en las pruebas automáticas.
 * Las contraseñas se guardan como hash SHA-256 con sal; nunca en texto plano.
 * Todas las operaciones corren en [Dispatchers.IO] para no bloquear la interfaz.
 */
class SqliteRepository(private val context: Context) : Repository {

    override val sourceName = "SQLite (modo local)"

    private val helper = DbHelper(context)
    private val db: SQLiteDatabase get() = helper.writableDatabase
    private val prefs = context.getSharedPreferences("local_session", Context.MODE_PRIVATE)

    // =====================================================================
    // Esquema de la base de datos
    // =====================================================================

    /** Crea las tablas y carga los datos de ejemplo la primera vez que se abre la base de datos. */
    private class DbHelper(context: Context) : SQLiteOpenHelper(context, "ironmarket.db", null, 1) {
        /** Crea las tablas y siembra los datos de ejemplo. */
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE users (id TEXT PRIMARY KEY, name TEXT, email TEXT UNIQUE, phone TEXT,
                role TEXT, active INTEGER, verified INTEGER, address TEXT, deleted INTEGER, password_hash TEXT)""")
            db.execSQL("""CREATE TABLE products (id TEXT PRIMARY KEY, name TEXT, category TEXT, price INTEGER,
                stock INTEGER, is_new INTEGER, description TEXT, invima TEXT, seller_id TEXT, seller_name TEXT,
                status TEXT, sold INTEGER, image_url TEXT, created_at INTEGER)""")
            db.execSQL("""CREATE TABLE cart (user_id TEXT, product_id TEXT, quantity INTEGER,
                PRIMARY KEY (user_id, product_id))""")
            db.execSQL("""CREATE TABLE orders (id TEXT PRIMARY KEY, buyer_id TEXT, buyer_name TEXT, seller_id TEXT,
                summary TEXT, total INTEGER, delivery TEXT, address TEXT, lat REAL, lng REAL, status TEXT,
                payment_ref TEXT, created_at INTEGER, category TEXT)""")
            seed(db)
        }

        /** Recrea las tablas al cambiar la versión de la base de datos. */
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            listOf("users", "products", "cart", "orders").forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
            onCreate(db)
        }

        /** Datos de ejemplo: las tres cuentas de prueba, productos y órdenes. */
        private fun seed(db: SQLiteDatabase) {
            val users = listOf(
                User("u1", "Diego Jiménez", "comprador@ironmarket.co", "3000000001", Role.COMPRADOR, address = "Cra. 13 # 63-20, Chapinero, Bogotá"),
                User("u2", "Suplementos La Fuerza", "vendedor@ironmarket.co", "3000000002", Role.VENDEDOR),
                User("u3", "Ana Morales", "admin@ironmarket.co", "3000000003", Role.ADMIN),
                User("u4", "Hierro de Segunda S.A.S.", "hierro@ironmarket.co", "3000000004", Role.VENDEDOR, verified = false),
                User("u5", "Laura Martínez", "laura@correo.com", "3000000005", Role.COMPRADOR),
                User("u6", "Carlos Ramírez", "carlos@correo.com", "3000000006", Role.COMPRADOR, active = false)
            )
            users.forEach { db.insert("users", null, userValues(it).apply { put("password_hash", hash(DemoData.DEMO_PASSWORD)) }) }
            DemoData.buildProducts("u2", "Suplementos La Fuerza") { UUID.randomUUID().toString() }
                .forEach { db.insert("products", null, productValues(it)) }
            val now = System.currentTimeMillis()
            listOf(
                Order("IM-10482", "u1", "Diego J.", "u2", "Whey Protein Isolate 2 lb ×1, Creatina 300 g ×1", 307_800, "Envío a domicilio", "Cra. 13 # 63-20, Bogotá", 4.6486, -74.0628, OrderStatus.NUEVO, "SANDBOX-8841203", now - 3_600_000, Category.SUPLEMENTOS),
                Order("IM-10479", "u5", "Laura M.", "u2", "Leggings deportivos mujer ×2", 159_800, "Recoge en tienda", "Tienda Chapinero", null, null, OrderStatus.NUEVO, "SANDBOX-8840977", now - 7_200_000, Category.ROPA),
                Order("IM-10466", "u1", "Diego J.", "u2", "Guantes de entrenamiento ×1", 67_900, "Envío a domicilio", "Cra. 13 # 63-20, Bogotá", null, null, OrderStatus.EN_CAMINO, "SANDBOX-8839012", now - 172_800_000, Category.ACCESORIOS),
                Order("IM-10431", "u5", "Laura M.", "u2", "Camiseta dry-fit ×3", 217_700, "Envío a domicilio", "Calle 100 # 15-30, Bogotá", null, null, OrderStatus.ENTREGADO, "SANDBOX-8830551", now - 691_200_000, Category.ROPA)
            ).forEach { db.insert("orders", null, orderValues(it)) }
        }
    }

    // =====================================================================
    // Autenticación
    // =====================================================================

    /** Registra una cuenta nueva y su perfil con el rol elegido. */
    override suspend fun register(name: String, email: String, phone: String, password: String, role: Role): User = io {
        if (findByEmail(email) != null) throw AppException("Este correo ya está registrado")
        val finalRole = if (isAdminEmail(email)) Role.ADMIN else role
        val user = User(UUID.randomUUID().toString(), name, email.trim().lowercase(), phone, finalRole,
            verified = finalRole != Role.VENDEDOR)
        db.insert("users", null, userValues(user).apply { put("password_hash", hash(password)) })
        prefs.edit().putString("uid", user.id).apply()
        user
    }

    /** Inicia sesión con correo y contraseña; rechaza cuentas suspendidas o eliminadas. */
    override suspend fun login(email: String, password: String): User = io {
        val c = db.query("users", null, "email = ?", arrayOf(email.trim().lowercase()), null, null, null)
        val pair = c.use { if (it.moveToFirst()) it.toUser() to it.str("password_hash") else null }
        val user = pair?.first
        if (user == null || pair.second != hash(password) || user.deleted) throw AppException("Correo o contraseña incorrectos")
        if (!user.active) throw AppException("Tu cuenta está suspendida. Contacta a soporte.")
        prefs.edit().putString("uid", user.id).apply()
        user
    }

    /** Envía el enlace para restablecer la contraseña al correo indicado. */
    override suspend fun sendPasswordReset(email: String) = io {
        // En modo local no hay servidor de correo: se simula el envío.
        Unit
    }

    /** Recupera el usuario de una sesión abierta anteriormente, o null. */
    override suspend fun restoreSession(): User? = io {
        prefs.getString("uid", null)?.let { findById(it) }?.takeIf { it.active && !it.deleted }
    }

    /** Cierra la sesión actual. */
    override fun logout() { prefs.edit().remove("uid").apply() }

    // =====================================================================
    // Usuarios
    // =====================================================================

    /** Lista todos los usuarios no eliminados. */
    override suspend fun getUsers(): List<User> = io {
        db.query("users", null, "deleted = 0", null, null, null, "name").use { c -> c.list { it.toUser() } }
    }

    /** Obtiene un usuario por su id, o null si no existe. */
    override suspend fun getUser(id: String): User? = io { findById(id) }

    /** Crea una cuenta desde el panel del administrador. */
    override suspend fun createUser(name: String, email: String, phone: String, role: Role, password: String): User = io {
        if (findByEmail(email) != null) throw AppException("Este correo ya está registrado")
        val user = User(UUID.randomUUID().toString(), name, email.trim().lowercase(), phone, role)
        db.insert("users", null, userValues(user).apply { put("password_hash", hash(password)) })
        user
    }

    /** Actualiza nombre, celular, rol, estado y verificación de un usuario. */
    override suspend fun updateUser(user: User) = io {
        db.update("users", userValues(user), "id = ?", arrayOf(user.id)); Unit
    }

    /** Eliminación lógica: anonimiza los datos personales y desactiva la cuenta. */
    override suspend fun deleteUser(id: String) = io {
        val v = ContentValues().apply {
            put("deleted", 1); put("active", 0); put("name", "Usuario eliminado"); put("phone", ""); put("address", "")
        }
        db.update("users", v, "id = ?", arrayOf(id)); Unit
    }

    // =====================================================================
    // Productos
    // =====================================================================

    /** Lista productos filtrando opcionalmente por estado y vendedor. */
    override suspend fun getProducts(status: ProductStatus?, sellerId: String?): List<Product> = io {
        val where = mutableListOf<String>(); val args = mutableListOf<String>()
        status?.let { where += "status = ?"; args += it.name }
        sellerId?.let { where += "seller_id = ?"; args += it }
        db.query("products", null, where.joinToString(" AND ").ifEmpty { null }, args.toTypedArray().ifEmpty { null },
            null, null, "created_at DESC").use { c -> c.list { it.toProduct() } }
    }

    /** Obtiene un producto por su id, o null si no existe. */
    override suspend fun getProduct(id: String): Product? = io {
        db.query("products", null, "id = ?", arrayOf(id), null, null, null).use { if (it.moveToFirst()) it.toProduct() else null }
    }

    /** Crea o actualiza un producto y guarda su foto si se envía una nueva. */
    override suspend fun saveProduct(product: Product, image: ByteArray?): Product = io {
        if (image != null) {
            // Las imágenes se guardan en el almacenamiento interno de la app
            val file = File(context.filesDir, "products/${product.id}.jpg").apply { parentFile?.mkdirs() }
            file.writeBytes(image)
            product.imageUrl = file.absolutePath
        }
        db.insertWithOnConflict("products", null, productValues(product), SQLiteDatabase.CONFLICT_REPLACE)
        product
    }

    /** Elimina un producto y su foto. */
    override suspend fun deleteProduct(id: String) = io {
        db.delete("products", "id = ?", arrayOf(id))
        db.delete("cart", "product_id = ?", arrayOf(id))
        File(context.filesDir, "products/$id.jpg").delete(); Unit
    }

    /** Genera un identificador nuevo para un producto. */
    override fun newProductId(): String = UUID.randomUUID().toString()

    // =====================================================================
    // Carrito
    // =====================================================================

    /** Devuelve el carrito del usuario con productos vigentes y cantidades ajustadas al stock. */
    override suspend fun getCart(userId: String): List<CartItem> = io {
        val rows = db.query("cart", null, "user_id = ?", arrayOf(userId), null, null, null)
            .use { c -> c.list { it.str("product_id") to it.getInt(it.getColumnIndexOrThrow("quantity")) } }
        rows.mapNotNull { (pid, qty) ->
            db.query("products", null, "id = ?", arrayOf(pid), null, null, null)
                .use { if (it.moveToFirst()) CartItem(it.toProduct(), qty) else null }
        }
    }

    /** Fija la cantidad de un producto en el carrito; 0 lo elimina. */
    override suspend fun setCartQuantity(userId: String, productId: String, quantity: Int) = io {
        if (quantity <= 0) db.delete("cart", "user_id = ? AND product_id = ?", arrayOf(userId, productId))
        else db.insertWithOnConflict("cart", null, ContentValues().apply {
            put("user_id", userId); put("product_id", productId); put("quantity", quantity)
        }, SQLiteDatabase.CONFLICT_REPLACE)
        Unit
    }

    /** Vacía el carrito del usuario. */
    override suspend fun clearCart(userId: String) = io { db.delete("cart", "user_id = ?", arrayOf(userId)); Unit }

    // =====================================================================
    // Órdenes
    // =====================================================================

    /** Registra una orden por vendedor, descuenta stock y vacía el carrito en una sola operación. */
    override suspend fun checkout(buyer: User, items: List<CartItem>, delivery: String, address: String,
                                  lat: Double?, lng: Double?, shipping: Int, paymentRef: String): List<Order> = io {
        val orders = mutableListOf<Order>()
        db.beginTransaction()   // todo o nada: órdenes, stock y carrito
        try {
            items.groupBy { it.product.sellerId }.forEach { (sellerId, group) ->
                val order = Order(
                    "IM-" + (System.currentTimeMillis() % 100000).toString().padStart(5, '0') + orders.size,
                    buyer.id, buyer.name, sellerId, group.joinToString { "${it.product.name} ×${it.quantity}" },
                    group.sumOf { it.subtotal } + shipping, delivery, address, lat, lng, OrderStatus.NUEVO,
                    paymentRef, System.currentTimeMillis(), group.first().product.category
                )
                db.insert("orders", null, orderValues(order))
                group.forEach {
                    db.execSQL("UPDATE products SET stock = stock - ?, sold = sold + ? WHERE id = ?",
                        arrayOf(it.quantity, it.quantity, it.product.id))
                }
                orders += order
            }
            db.delete("cart", "user_id = ?", arrayOf(buyer.id))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        orders
    }

    /** Lista órdenes filtrando opcionalmente por vendedor o comprador, de la más reciente a la más antigua. */
    override suspend fun getOrders(sellerId: String?, buyerId: String?): List<Order> = io {
        val (where, args) = when {
            sellerId != null -> "seller_id = ?" to arrayOf(sellerId)
            buyerId != null -> "buyer_id = ?" to arrayOf(buyerId)
            else -> null to null
        }
        db.query("orders", null, where, args, null, null, "created_at DESC").use { c -> c.list { it.toOrder() } }
    }

    /** Cambia el estado de una orden (por despachar, en camino, entregado). */
    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus) = io {
        db.update("orders", ContentValues().apply { put("status", status.name) }, "id = ?", arrayOf(orderId)); Unit
    }

    /** Carga productos de ejemplo a nombre del vendedor indicado. */
    override suspend fun seedDemoProducts(seller: User): Int = io {
        val list = DemoData.buildProducts(seller.id, seller.name) { UUID.randomUUID().toString() }
        list.forEach { db.insert("products", null, productValues(it)) }
        list.size
    }

    // =====================================================================
    // Utilidades privadas
    // =====================================================================

    /** Ejecuta el bloque en el hilo de entrada/salida. */
    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    /** Busca un usuario por id. */
    private fun findById(id: String) =
        db.query("users", null, "id = ?", arrayOf(id), null, null, null).use { if (it.moveToFirst()) it.toUser() else null }

    /** Busca un usuario por correo. */
    private fun findByEmail(email: String) =
        db.query("users", null, "email = ?", arrayOf(email.trim().lowercase()), null, null, null).use { if (it.moveToFirst()) it.toUser() else null }

    companion object {
        /** Hash SHA-256 con sal fija de la app (modo local de demostración). */
        fun hash(password: String): String =
            MessageDigest.getInstance("SHA-256").digest(("ironmarket:$password").toByteArray())
                .joinToString("") { "%02x".format(it) }

        /** Indica si el correo está configurado como administrador. */
        fun isAdminEmail(email: String) =
            BuildConfig.ADMIN_EMAILS.split(",").any { it.trim().equals(email.trim(), ignoreCase = true) }

        /** Valores de la tabla users para insertar o actualizar. */
        private fun userValues(u: User) = ContentValues().apply {
            put("id", u.id); put("name", u.name); put("email", u.email.lowercase()); put("phone", u.phone)
            put("role", u.role.name); put("active", if (u.active) 1 else 0); put("verified", if (u.verified) 1 else 0)
            put("address", u.address); put("deleted", if (u.deleted) 1 else 0)
        }

        /** Valores de la tabla products para insertar o actualizar. */
        private fun productValues(p: Product) = ContentValues().apply {
            put("id", p.id); put("name", p.name); put("category", p.category.name); put("price", p.price)
            put("stock", p.stock); put("is_new", if (p.isNew) 1 else 0); put("description", p.description)
            put("invima", p.invima); put("seller_id", p.sellerId); put("seller_name", p.sellerName)
            put("status", p.status.name); put("sold", p.sold); put("image_url", p.imageUrl); put("created_at", p.createdAt)
        }

        /** Valores de la tabla orders para insertar. */
        private fun orderValues(o: Order) = ContentValues().apply {
            put("id", o.id); put("buyer_id", o.buyerId); put("buyer_name", o.buyerName); put("seller_id", o.sellerId)
            put("summary", o.summary); put("total", o.total); put("delivery", o.delivery); put("address", o.address)
            put("lat", o.lat); put("lng", o.lng); put("status", o.status.name); put("payment_ref", o.paymentRef)
            put("created_at", o.createdAt); put("category", o.category.name)
        }

        /** Lee una columna de texto por nombre. */
        private fun Cursor.str(col: String): String = getString(getColumnIndexOrThrow(col)) ?: ""
        /** Lee una columna entera por nombre. */
        private fun Cursor.int(col: String): Int = getInt(getColumnIndexOrThrow(col))
        /** Lee una columna decimal por nombre. */
        private fun Cursor.dbl(col: String): Double? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getDouble(it) }
        /** Recorre el cursor y convierte cada fila. */
        private fun <T> Cursor.list(map: (Cursor) -> T): List<T> { val out = mutableListOf<T>(); while (moveToNext()) out += map(this); return out }

        /** Convierte el registro leído en un objeto User. */
        private fun Cursor.toUser() = User(str("id"), str("name"), str("email"), str("phone"), Role.valueOf(str("role")),
            int("active") == 1, int("verified") == 1, str("address"), int("deleted") == 1)

        /** Convierte el registro leído en un objeto Product. */
        private fun Cursor.toProduct() = Product(str("id"), str("name"), Category.valueOf(str("category")), int("price"),
            int("stock"), int("is_new") == 1, str("description"), str("invima").ifEmpty { null }, str("seller_id"),
            str("seller_name"), ProductStatus.valueOf(str("status")), int("sold"), str("image_url").ifEmpty { null },
            getLong(getColumnIndexOrThrow("created_at")))

        /** Convierte el registro leído en un objeto Order. */
        private fun Cursor.toOrder() = Order(str("id"), str("buyer_id"), str("buyer_name"), str("seller_id"), str("summary"),
            int("total"), str("delivery"), str("address"), dbl("lat"), dbl("lng"), OrderStatus.valueOf(str("status")),
            str("payment_ref"), getLong(getColumnIndexOrThrow("created_at")), Category.valueOf(str("category")))
    }
}
