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
import android.util.Log
import br.com.ticpass.pos.core.util.CommisionUtils
import br.com.ticpass.pos.core.util.NumericConversionUtils
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductAdapter(
    private val context: Context,
    private var products: List<ProductModel>
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    val numericConversionUtils = NumericConversionUtils
    val commissionUtils = CommisionUtils

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
        private val priceTextView: TextView = itemView.findViewById(R.id.productPrice)


        fun bind(product: ProductModel) {
            nameTextView.text = product.name
            val productPrice = product.price


            CoroutineScope(Dispatchers.Main).launch {
                val productPriceCommission = commissionUtils.calculateTotalWithCommission(productPrice)
                priceTextView.text = numericConversionUtils.convertLongToBrCurrencyString(productPriceCommission.toLong())
            }


            // Construir o nome do arquivo com extensão .webp (cada produto tem thumbnail = id do arquivo)
            val thumbnailFileName = if (product.thumbnail.endsWith(".webp")) {
                product.thumbnail
            } else {
                "${product.thumbnail}.webp"
            }

            val dir = File(context.filesDir, "thumbnails")
            val file = File(dir, thumbnailFileName)

            Log.d("ProductAdapter", "Tentando carregar thumbnail: $thumbnailFileName")
            Log.d("ProductAdapter", "Caminho do arquivo: ${file.absolutePath}, existe? ${file.exists()}")

            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    thumbnailImageView.setImageBitmap(bitmap)
                } else {
                    Log.w("ProductAdapter", "Bitmap nulo ao decodificar: ${file.absolutePath}")
                    thumbnailImageView.setImageResource(R.drawable.placeholder_image)
                }
            } else {
                // Caso não exista, colocar uma imagem padrão ou limpar
                thumbnailImageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }
}