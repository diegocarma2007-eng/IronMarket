package com.ironmarket.app.ui.buyer

import androidx.fragment.app.Fragment
import com.ironmarket.app.R
import com.ironmarket.app.data.Session
import com.ironmarket.app.ui.common.ProfileFragment
import com.ironmarket.app.ui.common.RoleHostActivity
import com.ironmarket.app.util.launchSafe

/** Pantalla principal del Comprador: Inicio, Catálogo, Carrito y Perfil. */
class BuyerActivity : RoleHostActivity() {
    override val menuRes = R.menu.menu_buyer

    /** Devuelve el fragmento que corresponde a la pestaña del menú inferior. */
    override fun fragmentFor(itemId: Int): Fragment = when (itemId) {
        R.id.nav_catalog -> CatalogFragment()
        R.id.nav_cart -> CartFragment()
        R.id.nav_profile -> ProfileFragment()
        else -> BuyerHomeFragment()
    }

    /** Actualiza la insignia del carrito al volver. */
    override fun onResume() {
        super.onResume()
        if (hasSession) refreshCartBadge()
    }

    /** Consulta el carrito y actualiza el número sobre el icono del menú. */
    fun refreshCartBadge() = launchSafe {
        val uid = Session.currentUser?.id ?: return@launchSafe
        setBadge(R.id.nav_cart, Session.repo.getCart(uid).sumOf { it.quantity })
    }
}
