package br.com.ticpass.pos.presentation.payment.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.core.util.NumericConversionUtils
import br.com.ticpass.pos.databinding.ItemCartBinding
import br.com.ticpass.pos.domain.shoppingCart.model.CartItemModel
import java.util.Locale

class PaymentSheetAdapter(
    private var items: List<CartItemModel>
) : RecyclerView.Adapter<PaymentSheetAdapter.CartItemViewHolder>() {

    private val numericConversionUtils = NumericConversionUtils

    inner class CartItemViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItemModel) {
            binding.tvName.text = item.product.name
            binding.tvQuantity.text = "Qtd: ${item.quantity}"
            binding.tvPrice.text = numericConversionUtils.convertLongToBrCurrencyString(item.product.price * item.quantity)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CartItemModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}