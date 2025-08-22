package br.com.ticpass.pos.view.ui.history.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.entity.ProductEntity
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ProductModalAdapter(
    private val productList: List<Pair<ProductEntity, Int>>
) : RecyclerView.Adapter<ProductModalAdapter.ProductModalViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductModalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_modal, parent, false)
        return ProductModalViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductModalViewHolder, position: Int) {
        val (product, quantity) = productList[position]
        holder.bind(product, quantity)
    }

    override fun getItemCount(): Int = productList.size

    inner class ProductModalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvProductQuantity: TextView = itemView.findViewById(R.id.tvProductQuantity)
        private val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)

        fun bind(product: ProductEntity, quantity: Int) {
            // Verifica se é um recurso drawable (começa com "@drawable/")
            if (product.thumbnail.startsWith("@drawable/")) {
                val resourceName = product.thumbnail.substringAfter("@drawable/")
                val resourceId = getDrawableResourceId(itemView.context, resourceName)

                if (resourceId != null) {
                    Glide.with(itemView.context)
                        .load(resourceId)
                        .into(ivProductImage)
                } else {
                    ivProductImage.setImageResource(R.drawable.ic_bitcoin_btc)
                }
            } else {
                Glide.with(itemView.context)
                    .load(product.thumbnail)
                    .placeholder(R.drawable.ic_bitcoin_btc)
                    .into(ivProductImage)
            }

            tvProductName.text = product.name
            tvProductQuantity.text = quantity.toString()

            // Cálculo do preço total
            val totalPrice = (product.price / 100.0) * quantity
            tvProductPrice.text = currencyFormat.format(totalPrice)
        }

        private fun getDrawableResourceId(context: android.content.Context, resourceName: String): Int? {
            return try {
                val resources = context.resources
                resources.getIdentifier(resourceName, "drawable", context.packageName)
            } catch (e: Exception) {
                null
            }
        }
    }
}