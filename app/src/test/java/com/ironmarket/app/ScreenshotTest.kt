package com.ironmarket.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ironmarket.app.data.Session
import com.ironmarket.app.ui.admin.AdminActivity
import com.ironmarket.app.ui.admin.UserFormActivity
import com.ironmarket.app.ui.auth.LoginActivity
import com.ironmarket.app.ui.auth.RecoverPasswordActivity
import com.ironmarket.app.ui.auth.RegisterActivity
import com.ironmarket.app.ui.auth.SplashActivity
import com.ironmarket.app.ui.buyer.BuyerActivity
import com.ironmarket.app.ui.buyer.CheckoutActivity
import com.ironmarket.app.ui.buyer.ProductDetailActivity
import com.ironmarket.app.ui.seller.ProductFormActivity
import com.ironmarket.app.ui.seller.SellerActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Pruebas de flujo con la base de datos SQLite y capturas de las pantallas reales de la app.
 * Las imágenes se guardan en app/build/screens y se usan en el manual de usuario.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xxhdpi")
class ScreenshotTest {

    private val out = File("build/screens").apply { mkdirs() }
    private val context get() = ApplicationProvider.getApplicationContext<IronMarketApp>()

    /** Deja correr las corrutinas (hilo de IO + hilo principal) hasta que la interfaz se estabiliza. */
    private fun settle(ms: Long = 1500) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(50)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun shot(activity: Activity, name: String) {
        settle()
        val v = activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun <T : Activity> open(cls: Class<T>, intent: Intent? = null): T {
        val i = intent ?: Intent(context, cls)
        return Robolectric.buildActivity(cls, i).setup().get()
    }

    private fun login(email: String) = runBlocking {
        Session.currentUser = Session.repo.login(email, "123456")
    }

    private fun tab(activity: Activity, id: Int) {
        activity.findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = id
        settle()
    }

    @Test
    fun flujosYCapturas() {
        // ---------- Transversal ----------
        shot(open(SplashActivity::class.java), "01_splash")
        val loginScreen = open(LoginActivity::class.java)
        loginScreen.findViewById<android.view.View>(R.id.chipBuyer).performClick()
        shot(loginScreen, "02_login")
        shot(open(RegisterActivity::class.java), "03_registro")
        val recover = open(RecoverPasswordActivity::class.java)
        shot(recover, "04_recuperar")

        // ---------- Comprador ----------
        login("comprador@ironmarket.co")
        assertEquals(com.ironmarket.app.data.Role.COMPRADOR, Session.role)
        val products = runBlocking { Session.repo.getProducts(com.ironmarket.app.data.ProductStatus.PUBLICADO) }
        assertTrue("Debe haber productos publicados", products.isNotEmpty())
        val buyer = open(BuyerActivity::class.java)
        shot(buyer, "05_comprador_inicio")
        tab(buyer, R.id.nav_catalog); shot(buyer, "06_comprador_catalogo")
        val whey = products.first { it.name.startsWith("Whey") }
        shot(open(ProductDetailActivity::class.java, Intent(context, ProductDetailActivity::class.java)
            .putExtra(ProductDetailActivity.EXTRA_ID, whey.id)), "07_comprador_detalle")
        // CRUD de carrito: agregar, actualizar cantidad
        runBlocking {
            val uid = Session.currentUser!!.id
            Session.repo.setCartQuantity(uid, whey.id, 1)
            Session.repo.setCartQuantity(uid, products.first { it.name.startsWith("Creatina") }.id, 2)
            Session.repo.setCartQuantity(uid, products.first { it.name.startsWith("Par mancuernas") }.id, 1)
            assertEquals(4, Session.repo.getCart(uid).sumOf { it.quantity })
        }
        tab(buyer, R.id.nav_cart); shot(buyer, "08_comprador_carrito")
        shot(open(CheckoutActivity::class.java), "09_comprador_pago")
        // Orden: registro de la compra con referencia de pago
        val orders = runBlocking {
            val u = Session.currentUser!!
            Session.repo.checkout(u, Session.repo.getCart(u.id), "Envío a domicilio", "Cra. 13 # 63-20, Bogotá", 4.6486, -74.0628, 8000, "SANDBOX-TEST")
        }
        assertTrue(orders.isNotEmpty())
        assertTrue(runBlocking { Session.repo.getCart(Session.currentUser!!.id) }.isEmpty())
        tab(buyer, R.id.nav_profile); shot(buyer, "10_comprador_perfil")

        // ---------- Vendedor ----------
        login("vendedor@ironmarket.co")
        val seller = open(SellerActivity::class.java)
        shot(seller, "11_vendedor_productos")
        shot(open(ProductFormActivity::class.java), "12_vendedor_nuevo_producto")
        tab(seller, R.id.nav_orders); shot(seller, "13_vendedor_pedidos")
        tab(seller, R.id.nav_profile); shot(seller, "14_vendedor_perfil")

        // ---------- Administrador ----------
        login("admin@ironmarket.co")
        val admin = open(AdminActivity::class.java)
        shot(admin, "15_admin_dashboard")
        tab(admin, R.id.nav_users); shot(admin, "16_admin_usuarios")
        shot(open(UserFormActivity::class.java), "17_admin_nuevo_usuario")
        tab(admin, R.id.nav_products); shot(admin, "18_admin_productos")
        tab(admin, R.id.nav_sales); shot(admin, "19_admin_ventas")

        // CRUD de usuarios: crear, actualizar y borrado lógico
        runBlocking {
            val u = Session.repo.createUser("Prueba QA", "qa@ironmarket.co", "3001112233", com.ironmarket.app.data.Role.COMPRADOR, "123456")
            u.phone = "3009998877"; Session.repo.updateUser(u)
            assertEquals("3009998877", Session.repo.getUser(u.id)!!.phone)
            Session.repo.deleteUser(u.id)
            assertTrue(Session.repo.getUsers().none { it.id == u.id })
        }
    }
}
