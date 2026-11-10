package com.ironmarket.app.ui.buyer

import androidx.fragment.app.Fragment
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.ui.common.ProfileFragment
import com.ironmarket.app.ui.common.RoleHostActivity

/** Pantalla principal del Comprador: Inicio, Catálogo, Carrito y Perfil. */
class BuyerActivity : RoleHostActivity() {
    override val menuRes = R.menu.menu_buyer

    override fun fragmentFor(itemId: Int): Fragment = when (itemId) {
        R.id.nav_catalog -> CatalogFragment()
        R.id.nav_cart -> CartFragment()
        R.id.nav_profile -> ProfileFragment()
        else -> BuyerHomeFragment()
    }

    override fun onResume() {
        super.onResume()
        refreshCartBadge()
    }

    fun refreshCartBadge() = setBadge(R.id.nav_cart, DataRepository.cartCount())
}
