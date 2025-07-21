package br.com.ticpass.pos.view.ui.products.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.Product
import com.bumptech.glide.Glide
import java.math.BigInteger
import java.util.Locale

fun formatCurrency(value: BigInteger): String {
    val locale = Locale("pt", "BR")
    val formatter = java.text.NumberFormat.getCurrencyInstance(locale)
    formatter.minimumFractionDigits = 2
    return formatter.format(value.toDouble() / 100)
}
class ProductsAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductsAdapter.VH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_products, parent, false)
        return VH(view, onItemClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onItemClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.productTitle)
        private val price = itemView.findViewById<TextView>(R.id.productPrice)
        private val img   = itemView.findViewById<ImageView>(R.id.productImage)

        fun bind(p: Product) {
            title.text = p.title
            price.text = formatCurrency(p.value)
            Glide.with(img).load(p.photo).into(img)

            itemView.setOnClickListener {
                onItemClick(p)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }

}