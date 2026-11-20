package com.ironmarket.app.data

/**
 * Datos de ejemplo para la demostración: se cargan en SQLite al crear la base de datos
 * y en Firestore desde el panel del administrador ("Cargar productos de ejemplo").
 */
object DemoData {

    /** Contraseña de las cuentas de prueba. */
    const val DEMO_PASSWORD = "123456"

    /** Plantillas de productos: (nombre, categoría, precio, stock, nuevo, descripción, INVIMA, vendidos). */
    val products = listOf(
        listOf("Whey Protein Isolate 2 lb", Category.SUPLEMENTOS, 189_900, 18, true,
            "Proteína aislada de suero con 27 g de proteína por servicio. Sabor vainilla. Lote L2409, vence 03/2028.", "RSA-0012345-2024", 312),
        listOf("Creatina monohidratada 300 g", Category.SUPLEMENTOS, 109_900, 12, true,
            "Creatina micronizada, 60 servicios de 5 g. Sin sabor. Lote C1130, vence 11/2027.", "RSA-0067890-2025", 205),
        listOf("Par mancuernas hexagonales 10 kg", Category.EQUIPO, 150_000, 2, false,
            "Mancuernas de caucho en buen estado, poco uso. Recogida en tienda o envío coordinado.", null, 41),
        listOf("Guantes de entrenamiento talla M", Category.ACCESORIOS, 59_900, 9, true,
            "Guantes con muñequera y palma acolchada.", null, 88),
        listOf("Camiseta dry-fit hombre", Category.ROPA, 69_900, 25, true,
            "Tela transpirable de secado rápido. Colores negro y gris.", null, 120),
        listOf("Banco plano ajustable (usado)", Category.EQUIPO, 320_000, 1, false,
            "Banco con 6 posiciones, soporta 250 kg. Detalles mínimos de pintura.", null, 9),
        listOf("Leggings deportivos mujer", Category.ROPA, 79_900, 14, true,
            "Tiro alto, tela compresiva y bolsillo lateral.", null, 97),
        listOf("Cinturón de levantamiento en cuero", Category.ACCESORIOS, 95_000, 6, true,
            "Cinturón de 10 cm de ancho con hebilla doble. Tallas S a XL.", null, 0)
    )

    /** Construye los productos de ejemplo asignados a un vendedor. */
    fun buildProducts(sellerId: String, sellerName: String, newId: () -> String): List<Product> =
        products.mapIndexed { i, p ->
            Product(
                id = newId(), name = p[0] as String, category = p[1] as Category, price = p[2] as Int,
                stock = p[3] as Int, isNew = p[4] as Boolean, description = p[5] as String,
                invima = p[6] as String?, sellerId = sellerId, sellerName = sellerName,
                // El último queda en revisión para mostrar el flujo de moderación
                status = if (i == products.lastIndex) ProductStatus.EN_REVISION else ProductStatus.PUBLICADO,
                sold = p[7] as Int, createdAt = System.currentTimeMillis() - i * 60_000L
            )
        }
}
