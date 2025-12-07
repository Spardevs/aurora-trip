package br.com.ticpass.pos.presentation.product.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.domain.product.model.ProductModel
import java.io.File

class ProductAdapter(
    private val context: Context,
    private var products: List<ProductModel>
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    fun updateProducts(newProducts: List<ProductModel>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_products, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.imageThumbnail)
        private val nameTextView: TextView = itemView.findViewById(R.id.textProductName)

        fun bind(product: ProductModel) {
            nameTextView.text = product.name

            // Carregar a imagem da thumbnail do diretório thumbnails
            val thumbnailFileName = product.thumbnail // id que é o nome do arquivo
            val dir = File(context.filesDir, "thumbnails")
            val file = File(dir, thumbnailFileName)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                thumbnailImageView.setImageBitmap(bitmap)
            } else {
                // Caso não exista, colocar uma imagem padrão ou limpar
                thumbnailImageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }
}