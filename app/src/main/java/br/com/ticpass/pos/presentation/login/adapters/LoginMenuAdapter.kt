package br.com.ticpass.pos.presentation.login.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.domain.menu.model.Menu
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class LoginMenuAdapter(
    private val items: List<Menu>,
    private val onRequestLogo: (menuId: String, rawLogo: String?) -> Unit,
    private val logos: Map<String, File>? = null,
    private val onClick: (Menu) -> Unit
) : RecyclerView.Adapter<LoginMenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_login_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menu = items[position]
        val logoFile = logos?.get(menu.id) ?: menu.logo?.let { path ->
            val f = File(path)
            if (f.exists()) f else null
        }

        holder.bind(menu, logoFile)
        holder.itemView.setOnClickListener { onClick(items[position]) }

        if (logoFile == null && !menu.logo.isNullOrBlank()) {
            onRequestLogo(menu.id, menu.logo)
        }
    }

    override fun getItemCount(): Int = items.size

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val menuName: TextView = itemView.findViewById(R.id.menuName)
        private val menuImage: ImageView = itemView.findViewById(R.id.menuImage)
        private val menuDateStart: TextView = itemView.findViewById(R.id.menuDateStart)
        private val menuDateEnd: TextView = itemView.findViewById(R.id.menuDateEnd)

        fun bind(menu: Menu, logoFile: File?) {
            // Use domain model properties
            menuName.text = menu.label
            menuDateStart.text = formatDate(menu.date.start)
            menuDateEnd.text = formatDate(menu.date.end)

            // Only use logoFile if it exists, otherwise show placeholder
            // Don't try to load menu.logo as it's just an ID, not a URL or file path
            val imageSource: Any = when {
                logoFile != null && logoFile.exists() -> logoFile
                else -> R.drawable.placeholder_image // Fallback placeholder
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