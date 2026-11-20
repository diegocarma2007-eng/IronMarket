package com.ironmarket.app.ui.common

import android.app.Activity
import android.content.Intent
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.Session
import com.ironmarket.app.data.User
import com.ironmarket.app.ui.admin.AdminActivity
import com.ironmarket.app.ui.auth.LoginActivity
import com.ironmarket.app.ui.buyer.BuyerActivity
import com.ironmarket.app.ui.seller.SellerActivity

/** Navegación entre los paneles de cada rol. */
object Navigation {

    /** Guarda el usuario en la sesión y abre el panel de su rol, limpiando la pila de pantallas. */
    fun openHome(activity: Activity, user: User) {
        Session.currentUser = user
        val target = when (user.role) {
            Role.COMPRADOR -> BuyerActivity::class.java
            Role.VENDEDOR -> SellerActivity::class.java
            Role.ADMIN -> AdminActivity::class.java
        }
        activity.startActivity(Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    /** Cierra la sesión y vuelve al inicio de sesión. */
    fun logout(activity: Activity) {
        Session.logout()
        activity.startActivity(Intent(activity, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
}
