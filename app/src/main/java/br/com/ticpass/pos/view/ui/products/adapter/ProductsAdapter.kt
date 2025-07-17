package br.com.ticpass.pos.view.ui.products.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.Product
import com.bumptech.glide.Glide

class ProductsAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_products, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.productImage)
        private val tvName: TextView  = itemView.findViewById(R.id.productTitle)
        private val tvPrice: TextView = itemView.findViewById(R.id.productPrice)

        @SuppressLint("SetTextI18n")
        fun bind(product: Product) {
            tvName.text  = product.title
            tvPrice.text = "R$ %.2f".format(product.value.toDouble())
            Glide.with(itemView)
                .load(product.photo)
                .centerCrop()
                .into(ivPhoto)

            itemView.setOnClickListener { onItemClick(product) }
        }
    }
}
