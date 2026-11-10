package com.ironmarket.app.ui.seller

import androidx.fragment.app.Fragment
import com.ironmarket.app.R
import com.ironmarket.app.ui.common.ProfileFragment
import com.ironmarket.app.ui.common.RoleHostActivity

/** Pantalla principal del Vendedor: Productos, Pedidos y Perfil. */
class SellerActivity : RoleHostActivity() {
    override val menuRes = R.menu.menu_seller

    override fun fragmentFor(itemId: Int): Fragment = when (itemId) {
        R.id.nav_orders -> SellerOrdersFragment()
        R.id.nav_profile -> ProfileFragment()
        else -> SellerProductsFragment()
    }
}
