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
import br.com.ticpass.pos.core.util.ShoppingCartUtils
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductAdapter(
    private val context: Context,
    private var products: List<ProductModel>,
    private val onProductClick: (ProductModel) -> Unit,
    private val onProductLongClick: (ProductModel) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val numericConversionUtils = NumericConversionUtils
    private val commissionUtils = CommisionUtils

    // Mapa de productId para quantidade no carrinho
    private var cartQuantities: Map<String, Int> = emptyMap()

    fun updateProducts(newProducts: List<ProductModel>) {
        products = newProducts
        notifyDataSetChanged()
    }

    // Atualiza as quantidades do carrinho e notifica para atualizar badges
    fun updateCartQuantities(newCartQuantities: Map<String, Int>) {
        val oldCartQuantities = cartQuantities
        cartQuantities = newCartQuantities

        // Notificar apenas os itens que tiveram a quantidade alterada
        for (i in products.indices) {
            val product = products[i]
            val productId = product.id.toString()
            val oldQty = oldCartQuantities[productId] ?: 0
            val newQty = newCartQuantities[productId] ?: 0
            if (oldQty != newQty) {
                notifyItemChanged(i)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_products, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        val quantity = cartQuantities[product.id] ?: 0
        holder.bind(product, quantity)
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.imageThumbnail)
        private val nameTextView: TextView = itemView.findViewById(R.id.textProductName)
        private val priceTextView: TextView = itemView.findViewById(R.id.productPrice)
        private val productBadge: TextView = itemView.findViewById(R.id.productBadge)

        fun bind(product: ProductModel, quantity: Int) {
            nameTextView.text = product.name
            CoroutineScope(Dispatchers.Main).launch {
                val productPriceCommission = commissionUtils.calculateTotalWithCommission(product.price)
                priceTextView.text = numericConversionUtils.convertLongToBrCurrencyString(productPriceCommission.toLong())
            }

            if (quantity > 0) {
                productBadge.visibility = View.VISIBLE
                productBadge.text = quantity.toString()
            } else {
                productBadge.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onProductClick(product)
            }

            itemView.setOnLongClickListener {
                onProductLongClick(product)
                true
            }

            // Carregar thumbnail (sem alteração)
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
                thumbnailImageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }
}