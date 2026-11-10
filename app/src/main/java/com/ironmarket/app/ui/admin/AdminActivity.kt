package com.ironmarket.app.ui.admin

import androidx.fragment.app.Fragment
import com.ironmarket.app.R
import com.ironmarket.app.ui.common.RoleHostActivity

/** Pantalla principal del Administrador: Panel, Usuarios, Productos y Ventas. */
class AdminActivity : RoleHostActivity() {
    override val menuRes = R.menu.menu_admin

    override fun fragmentFor(itemId: Int): Fragment = when (itemId) {
        R.id.nav_users -> AdminUsersFragment()
        R.id.nav_products -> AdminProductsFragment()
        R.id.nav_sales -> AdminSalesFragment()
        else -> AdminDashboardFragment()
    }
}
