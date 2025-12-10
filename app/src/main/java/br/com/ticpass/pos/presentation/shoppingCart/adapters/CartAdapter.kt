package br.com.ticpass.pos.presentation.shoppingCart.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.domain.shoppingCart.model.CartItemModel
import com.bumptech.glide.Glide
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val onIncrease: (CartItemModel) -> Unit,
    private val onDecrease: (CartItemModel) -> Unit
) : ListAdapter<CartItemModel, CartAdapter.CartViewHolder>(Diff()) {

    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnIncrease: ImageButton = view.findViewById(R.id.btnIncrease)
        val btnDecrease: ImageButton = view.findViewById(R.id.btnDecrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.tvName.text = item.product.name
        holder.tvPrice.text = formatPrice(item.product.price)
        holder.tvQuantity.text = item.quantity.toString()

        val thumbName = item.product.thumbnail
        val thumbFile = File(context.filesDir, "thumbnails/$thumbName")

        if (thumbFile.exists()) {
            Glide.with(context)
                .load(thumbFile)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.imgThumbnail)
        } else {
            holder.imgThumbnail.setImageResource(R.drawable.placeholder_image)
        }

        holder.btnIncrease.setOnClickListener { onIncrease(item) }
        holder.btnDecrease.setOnClickListener { onDecrease(item) }
    }

    private fun formatPrice(priceInCents: Long): String {
        val value = priceInCents / 100.0
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            .format(value)
    }

    class Diff : DiffUtil.ItemCallback<CartItemModel>() {
        override fun areItemsTheSame(oldItem: CartItemModel, newItem: CartItemModel) =
            oldItem.product.id == newItem.product.id

        override fun areContentsTheSame(oldItem: CartItemModel, newItem: CartItemModel) =
            oldItem == newItem
    }
}