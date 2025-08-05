package br.com.ticpass.pos.view.ui.shoppingCart.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.entity.CartItem
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ShoppingCartAdapter(
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onObservationClick: (CartItem) -> Unit
) : RecyclerView.Adapter<ShoppingCartAdapter.ViewHolder>() {

    private val items = mutableListOf<CartItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(data: List<CartItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val img         = view.findViewById<ImageView>(R.id.imgThumbnail)
        private val name        = view.findViewById<TextView>(R.id.tvName)
        private val price       = view.findViewById<TextView>(R.id.tvPrice)
        private val quantity    = view.findViewById<TextView>(R.id.tvQuantity)
        private val btnIncrease = view.findViewById<ImageView>(R.id.btnIncrease)
        private val btnDecrease = view.findViewById<ImageView>(R.id.btnDecrease)
        private val btnDelete   = view.findViewById<ImageView>(R.id.btnDelete)
        private val btnObs = view.findViewById<ImageView>(R.id.btnObs)
        private val obsDescription = view.findViewById<TextView>(R.id.obsDescription)

        fun bind(item: CartItem, onUpdate: (CartItem, Int) -> Unit) {
            name.text     = item.product.name
            price.text    = formatCurrency(item.product.price)
            quantity.text = item.quantity.toString()
            btnObs.setOnClickListener { onObservationClick(item) }


            Glide.with(img.context)
                .load(item.product.thumbnail)
                .into(img)

            btnIncrease.setOnClickListener { onUpdate(item, item.quantity + 1) }
            btnDecrease.setOnClickListener {
                if (item.quantity > 1) onUpdate(item, item.quantity - 1)
            }
            btnDelete.setOnClickListener { onUpdate(item, 0) }

            val observation = item.observation
            if (!observation.isNullOrEmpty()) {
                obsDescription.text = observation
                obsDescription.visibility = View.VISIBLE
            } else {
                obsDescription.visibility = View.GONE
            }
        }

        private fun formatCurrency(value: Long): String {
            val fmt = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            return fmt.format(value / 100.0)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onQuantityChange)
    }

    override fun getItemCount(): Int = items.size
}
