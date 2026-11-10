package com.ironmarket.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ironmarket.app.R
import com.ironmarket.app.data.Role
import com.ironmarket.app.data.User
import com.ironmarket.app.databinding.ItemUserBinding
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.initials

/** Lista de usuarios para el administrador: editar, suspender/reactivar y eliminar. */
class UserAdapter(
    private var items: List<User>,
    private val onEdit: (User) -> Unit,
    private val onToggle: (User) -> Unit,
    private val onDelete: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.VH>() {

    class VH(val b: ItemUserBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<User>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = items[position]
        val ctx = holder.itemView.context
        val (bg, fg) = when (u.role) {
            Role.COMPRADOR -> R.color.im_lime_dark to R.color.im_lime
            Role.VENDEDOR -> R.color.im_orange_dark to R.color.im_orange
            Role.ADMIN -> R.color.im_blue_dark to R.color.im_blue
        }
        with(holder.b) {
            avatar.text = u.name.initials()
            avatar.backgroundTintList = ContextCompat.getColorStateList(ctx, bg)
            avatar.setTextColor(ContextCompat.getColor(ctx, fg))
            name.text = u.name
            email.text = u.email
            role.badge(u.role.label, bg, fg)
            when {
                !u.active -> state.badge("Suspendido", R.color.im_danger_dark, R.color.im_danger)
                u.role == Role.VENDEDOR && !u.verified -> state.badge("Verificar NIT", R.color.im_warning_dark, R.color.im_warning)
                else -> state.badge("Activo", R.color.im_surface_high, R.color.im_text_muted)
            }
            toggleButton.setIconResource(if (u.active) R.drawable.ic_block else R.drawable.ic_refresh)
            toggleButton.contentDescription = if (u.active) "Suspender usuario" else "Reactivar usuario"
            toggleButton.setOnClickListener { onToggle(u) }
            editButton.setOnClickListener { onEdit(u) }
            deleteButton.setOnClickListener { onDelete(u) }
        }
    }
}
