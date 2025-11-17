package br.com.ticpass.pos.view.ui.products.adapter

import android.annotation.SuppressLint
import android.content.Context
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
import br.com.ticpass.pos.util.ThumbnailManager
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ProductsAdapter(
    private val shoppingCartManager: ShoppingCartManager,
    private val onProductClicked: (Product) -> Unit
) : ListAdapter<Product, ProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    // Lista para controlar os observers dos ViewHolders
    private val viewHolderObservers = mutableMapOf<Int, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_products, parent, false)
        return ProductViewHolder(view, shoppingCartManager)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)

        // Configurar observer seguro
        val observerId = holder.setupCartObserver()
        viewHolderObservers[holder.hashCode()] = observerId

        holder.itemView.setOnClickListener {
            onProductClicked(product)
            holder.updateBadge()
        }
    }

    override fun onViewRecycled(holder: ProductViewHolder) {
        super.onViewRecycled(holder)
        holder.clearObservers()
        // Remover da lista de controle
        viewHolderObservers.remove(holder.hashCode())?.let { observerId ->
            shoppingCartManager.removeSafeObserver(observerId)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearAll() {
        // Limpar todos os observers dos ViewHolders
        viewHolderObservers.values.forEach { observerId ->
            shoppingCartManager.removeSafeObserver(observerId)
        }
        viewHolderObservers.clear()
        notifyDataSetChanged()
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
        private var observerId: String? = null

        init {
            itemView.setOnLongClickListener {
                currentProduct?.let { product ->
                    // aqui depende do seu ShoppingCartManager: atualiza pelo id do produto
                    shoppingCartManager.updateItem(product.id, 0)
                    Toast.makeText(itemView.context, "Itens excluidos do carrinho", Toast.LENGTH_SHORT).show()
                    true
                } ?: false
            }
        }

        fun setupCartObserver(): String {
            val observer = androidx.lifecycle.Observer<Any> {
                updateBadge()
            }
            observerId = shoppingCartManager.observeForeverSafe(observer)
            return observerId!!
        }

        fun bind(product: Product) {
            currentProduct = product

            // API Product usa 'label'
            nameTextView.text = product.label

            // API Product usa 'price' (Int). Converter pra Long e formatar como reais.
            priceTextView.text = formatCurrencyFromCents(product.price.toLong())

            updateBadge()

            // Carrega thumbnail local (se existir) ou placeholder.
            // A API devolve um ProductThumbnail com um id; usamos esse id para procurar o arquivo local.
            val thumbnailId = product.thumbnail?.id ?: ""
            if (thumbnailId.isNotEmpty()) {
                loadThumbnail(itemView.context, thumbnailId)
            } else {
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }

        private fun loadThumbnail(context: Context, thumbnailId: String) {
            val thumbnailFile = ThumbnailManager.getThumbnailFile(context, thumbnailId)
            if (thumbnailFile != null && thumbnailFile.exists()) {
                Glide.with(context)
                    .load(thumbnailFile)
                    .placeholder(R.drawable.placeholder_image)
                    .into(imageView)
            } else {
                // Se não houver arquivo local, mostrar placeholder (poderíamos também tentar uma URL remota se houver)
                imageView.setImageResource(R.drawable.placeholder_image)
            }
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

        fun clearObservers() {
            observerId?.let {
                shoppingCartManager.removeSafeObserver(it)
                observerId = null
            }
            currentProduct = null
        }

        // Converte centavos (Long) para reais e formata no Locale pt-BR
        private fun formatCurrencyFromCents(valueInCents: Long): String {
            // Se price da API é em centavos, divide por 100. Ajuste se seu valor usar outra escala.
            val valueInReais = valueInCents.toDouble() / 100.0
            val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            return format.format(valueInReais)
        }

    }

    fun updateProducts(newProducts: List<Product>) {
        submitList(newProducts)
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