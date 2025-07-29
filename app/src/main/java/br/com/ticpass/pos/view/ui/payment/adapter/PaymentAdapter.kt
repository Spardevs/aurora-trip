package br.com.ticpass.pos.view.ui.payment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.viewmodel.payment.PaymentMethod

class PaymentAdapter(
    private val paymentMethods: List<PaymentMethod>,
    private val onItemSelected: (PaymentMethod) -> Unit
) : RecyclerView.Adapter<PaymentAdapter.PaymentMethodViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_method, parent, false)
        return PaymentMethodViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentMethodViewHolder, position: Int) {
        holder.bind(paymentMethods[position])
    }

    override fun getItemCount(): Int = paymentMethods.size

    inner class PaymentMethodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(method: PaymentMethod) {
            itemView.apply {
                findViewById<ImageView>(R.id.iv_payment_icon).setImageResource(method.iconRes)
                findViewById<TextView>(R.id.tv_payment_name).text = method.name

                setOnClickListener { onItemSelected(method) }
            }
        }
    }
}