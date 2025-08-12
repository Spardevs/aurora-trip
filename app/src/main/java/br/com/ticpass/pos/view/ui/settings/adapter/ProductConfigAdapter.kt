package br.com.ticpass.pos.view.ui.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.data.room.entity.ProductEntity
import br.com.ticpass.pos.databinding.ItemProductConfigBinding
import com.bumptech.glide.Glide

class ProductConfigAdapter(
    private val onToggle: (ProductEntity) -> Unit
) : ListAdapter<ProductEntity, ProductConfigAdapter.ProductConfigViewHolder>(ProductDiffCallback()) {

    private var loadingItemId: Long? = null

    fun setLoadingItemId(id: Long?) {
        loadingItemId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductConfigViewHolder {
        val binding = ItemProductConfigBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductConfigViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductConfigViewHolder, position: Int) {
        val product = getItem(position)
        val isLoading = product.id == loadingItemId.toString()
        holder.bind(product, isLoading, onToggle)
    }

    inner class ProductConfigViewHolder(
        private val binding: ItemProductConfigBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity, isLoading: Boolean, onToggle: (ProductEntity) -> Unit) {
            binding.apply {
                if (isLoading) {
                    showSkeleton()
                } else {
                    hideSkeleton()
                    productName.text = product.name
                    productPrice.text = "R$ ${"%.2f".format(product.price / 100.0).replace(".", ",")}"

                    Glide.with(root.context)
                        .load(product.thumbnail)
                        .centerCrop()
                        .into(productImage)

                    // Remove previous listener to avoid multiple triggers
                    toggleButton.setOnCheckedChangeListener(null)
                    toggleButton.isChecked = product.isEnabled

                    toggleButton.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != product.isEnabled && !isLoading) {
                            onToggle(product.copy(isEnabled = isChecked))
                        }
                    }
                }
            }
        }

        private fun showSkeleton() {
            binding.apply {
                productName.visibility = View.INVISIBLE
                productPrice.visibility = View.INVISIBLE
                productImage.visibility = View.INVISIBLE

                productNameSkeleton.visibility = View.VISIBLE
                productPriceSkeleton.visibility = View.VISIBLE
                productImageSkeleton.visibility = View.VISIBLE
                toggleButton.isEnabled = false
            }
        }

        private fun hideSkeleton() {
            binding.apply {
                productName.visibility = View.VISIBLE
                productPrice.visibility = View.VISIBLE
                productImage.visibility = View.VISIBLE

                productNameSkeleton.visibility = View.GONE
                productPriceSkeleton.visibility = View.GONE
                productImageSkeleton.visibility = View.GONE
                toggleButton.isEnabled = true
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem == newItem
        }
    }
}