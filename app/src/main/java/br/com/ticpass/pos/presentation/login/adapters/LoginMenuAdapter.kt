// file: LoginMenuAdapter.kt
package br.com.ticpass.pos.presentation.login.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.domain.menu.model.MenuDb
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class LoginMenuAdapter(
    private val items: List<MenuDb>,
    private val logos: Map<String, File>,
    private val onClick: (MenuDb) -> Unit
) : RecyclerView.Adapter<LoginMenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_login_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menu = items[position]
        holder.bind(menu, logos[menu.id])
        holder.itemView.setOnClickListener { onClick(menu) }
    }

    override fun getItemCount(): Int = items.size

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val menuName: TextView = itemView.findViewById(R.id.menuName)
        private val menuImage: ImageView = itemView.findViewById(R.id.menuImage)
        private val menuDateStart: TextView = itemView.findViewById(R.id.menuDateStart)
        private val menuDateEnd: TextView = itemView.findViewById(R.id.menuDateEnd)

        fun bind(menu: MenuDb, logoFile: File?) {
            menuName.text = menu.name
            menuDateStart.text = formatDate(menu.dateStart)
            menuDateEnd.text = formatDate(menu.dateEnd)

            val imageSource = when {
                logoFile != null && logoFile.exists() -> logoFile
                !menu.logo.isNullOrEmpty() -> menu.logo // url or id
                else -> R.drawable.icon
            }

            Glide.with(itemView.context)
                .load(imageSource)
                .placeholder(R.drawable.icon)
                .error(R.drawable.icon)
                .into(menuImage)
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM 'às' HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: return dateString)
            } catch (e: Exception) {
                dateString
            }
        }
    }
}