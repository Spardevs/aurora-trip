package br.com.ticpass.pos.view.ui.products.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.Product
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ProductsAdapter(
    private val shoppingCartManager: ShoppingCartManager,
    private val onProductClicked: (Product) -> Unit
) : ListAdapter<Product, ProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_products, parent, false)
        return ProductViewHolder(view, shoppingCartManager)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
        holder.itemView.setOnClickListener {
            onProductClicked(product)
            holder.updateBadge()
        }
    }

    inner class ProductViewHolder(
        itemView: View,
        private val shoppingCartManager: ShoppingCartManager
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.productTitle)
        private val priceTextView: TextView = itemView.findViewById(R.id.productPrice)
        private val imageView: ImageView = itemView.findViewById(R.id.productImage)
        private val badgeTextView: TextView = itemView.findViewById(R.id.productBadge)
        private var currentProduct: Product? = null

        init {
            shoppingCartManager.cartUpdates.observeForever {
                currentProduct?.let { updateBadge() }
            }

            shoppingCartManager.cartUpdates.observeForever {
                currentProduct?.let { updateBadge() }
            }

            itemView.setOnLongClickListener {
                currentProduct?.let { product ->
                    shoppingCartManager.updateItem(product.id, 0)
                    Toast.makeText(itemView.context, "Itens excluidos do carrinho", Toast.LENGTH_SHORT).show()
                    true
                } ?: false
            }
        }

        fun bind(product: Product) {
            currentProduct = product
            nameTextView.text = product.title
            priceTextView.text = formatCurrency(product.value.toDouble())
            updateBadge()

            Glide.with(itemView.context)
                .load(product.photo)
                .into(imageView)
        }

        fun updateBadge() {
            currentProduct?.let { product ->
                val quantity = shoppingCartManager.getCart().items[product.id] ?: 0
                if (quantity > 0) {
                    badgeTextView.text = quantity.toString()
                    badgeTextView.visibility = View.VISIBLE
                } else {
                    badgeTextView.visibility = View.GONE
                }
            }
        }



        private fun formatCurrency(value: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            return format.format(value)
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}