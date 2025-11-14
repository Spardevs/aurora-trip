package br.com.ticpass.pos.view.ui.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.Menu
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MenuScreen(
    private val menus: List<Menu>,
    private val onItemClicked: (Menu) -> Unit
) : RecyclerView.Adapter<MenuScreen.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menu = menus[position]
        holder.bind(menu)
        holder.itemView.setOnClickListener { onItemClicked(menu) }
    }

    override fun getItemCount(): Int = menus.size

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val menuName: TextView = itemView.findViewById(R.id.menuName)
        private val menuImage: ImageView = itemView.findViewById(R.id.menuImage)
        private val menuDateStart: TextView = itemView.findViewById(R.id.menuDateStart)
        private val menuDateEnd: TextView = itemView.findViewById(R.id.menuDateEnd)

        fun bind(menu: Menu) {
            menuName.text = menu.name
            menuDateStart.text = "${formatDate(menu.dateStart)}"
            menuDateEnd.text = "${formatDate(menu.dateEnd)}"

            // ✅ Verifica se existe logo local usando logoId
            val logoId = menu.logoId ?: menu.id
            val logoFile = File(itemView.context.filesDir, "MenusLogo/$logoId.png")
            val imageSource = if (logoFile.exists()) {
                logoFile
            } else if (menu.imageUrl.isNotEmpty()) {
                menu.imageUrl
            } else {
                R.drawable.icon // Fallback para @drawable/icon
            }

            Glide.with(itemView.context)
                .load(imageSource)
                .placeholder(R.drawable.icon)
                .error(R.drawable.icon)
                .into(menuImage)
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM 'às' HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date)
            } catch (e: Exception) {
                dateString
            }
        }
    }
}