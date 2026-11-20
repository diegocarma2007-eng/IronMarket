package com.ironmarket.app

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ironmarket.app.data.Session
import com.ironmarket.app.ui.buyer.CheckoutActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast

/**
 * Regresión: después de un pago aprobado debe mostrarse el diálogo de confirmación.
 * Antes, el tema de los diálogos tenía una referencia circular (colorPrimary) y el diálogo fallaba sin mostrarse.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CheckoutDialogTest {
    /** Simula el regreso aprobado de ePayco y verifica que aparece «¡Pago aprobado!» sin errores. */
    @Test
    fun dialogoTrasPago() {
        ApplicationProvider.getApplicationContext<IronMarketApp>()
        runBlocking {
            val u = Session.repo.login("comprador@ironmarket.co", "123456"); Session.currentUser = u
            val p = Session.repo.getProducts(null, null).first { it.stock > 0 }
            Session.repo.setCartQuantity(u.id, p.id, 1)
        }
        val a = Robolectric.buildActivity(CheckoutActivity::class.java).setup().get()
        repeat(30) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(50) }
        val m = CheckoutActivity::class.java.getDeclaredMethod("registerOrders", String::class.java, String::class.java)
        m.isAccessible = true
        m.invoke(a, "SANDBOX-1", "Aceptada (simulada)")
        repeat(40) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(50) }
        assertNull("No debe mostrarse un error", ShadowToast.getTextOfLatestToast())
        assertNotNull("Debe mostrarse el diálogo de pago aprobado", ShadowDialog.getLatestDialog())
    }
}
