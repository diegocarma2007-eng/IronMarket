package com.ironmarket.app.ui.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.ironmarket.app.R
import com.ironmarket.app.databinding.ActivityRoleHostBinding

/**
 * Contenedor común de los tres roles: un FrameLayout para los fragmentos
 * y un BottomNavigationView con el menú propio de cada rol.
 */
abstract class RoleHostActivity : RoleActivity() {

    protected lateinit var binding: ActivityRoleHostBinding

    /** Menú XML del rol (res/menu). */
    abstract val menuRes: Int

    /** Fragmento que corresponde a cada opción del menú. */
    abstract fun fragmentFor(itemId: Int): Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.inflateMenu(menuRes)
        binding.bottomNav.setOnItemSelectedListener { item ->
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragmentFor(item.itemId))
                .commit()
            true
        }
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = binding.bottomNav.menu.getItem(0).itemId
        }
    }

    /** Permite a un fragmento cambiar de pestaña (por ejemplo, de Inicio a Catálogo). */
    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    /** Muestra un número sobre un ítem del menú (por ejemplo, productos en el carrito). */
    fun setBadge(itemId: Int, count: Int) {
        if (count > 0) binding.bottomNav.getOrCreateBadge(itemId).apply {
            number = count
            backgroundColor = getColor(R.color.im_orange)
            badgeTextColor = getColor(R.color.im_ink)
        } else binding.bottomNav.removeBadge(itemId)
    }
}
