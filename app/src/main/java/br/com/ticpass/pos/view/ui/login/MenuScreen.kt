package br.com.ticpass.pos.view.ui.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.Menu

class MenuScreen(
    private val menus: List<Menu>,
    private val onClick: (Menu) -> Unit
) : RecyclerView.Adapter<MenuScreen.MenuViewHolder>() {

    inner class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.menuTitle)
        private val description = itemView.findViewById<TextView>(R.id.menuDescription)

        fun bind(menu: Menu) {
            title.text = menu.title
            description.text = menu.description
            itemView.setOnClickListener { onClick(menu) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menus[position])
    }

    override fun getItemCount() = menus.size
}