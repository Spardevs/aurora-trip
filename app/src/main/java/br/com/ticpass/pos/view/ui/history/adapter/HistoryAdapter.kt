package br.com.ticpass.pos.view.ui.history.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.data.model.History
import br.com.ticpass.pos.R
import java.text.SimpleDateFormat
import java.util.Locale

// Função de extensão fora da classe
fun String.getPaymentMethodIcon(): Int {
    return when (lowercase(Locale.getDefault())) {
        "cartão de crédito", "crédito", "credit" -> R.drawable.credit
        "cartão de débito", "débito", "debit" -> R.drawable.debit
        "pix" -> R.drawable.pix
        "dinheiro", "cash" -> R.drawable.cash
        "vale refeição", "vr" -> R.drawable.vr
        "bitcoin", "btc" -> R.drawable.ic_bitcoin_btc
        else -> R.drawable.cash
    }
}

class HistoryAdapter(
    private val historyList: List<History>,
    private val onItemClick: (History) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.bind(history)
        holder.itemView.setOnClickListener { onItemClick(history) }
    }

    override fun getItemCount(): Int = historyList.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)
        private val tvPaymentPrice: TextView = itemView.findViewById(R.id.tvPaymentPrice)
        private val tvCommissionPrice: TextView = itemView.findViewById(R.id.tvCommissionPrice)
        private val tvPaymentType: TextView = itemView.findViewById(R.id.tvPaymentType)
        private val tvIdTransaction: TextView = itemView.findViewById(R.id.tvIdTransaction)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private  val tvAtk: TextView = itemView.findViewById(R.id.tvAtk)

        fun bind(history: History) {
            tvTotalPrice.text = "R$ ${String.format(Locale.getDefault(), "%.2f", history.totalPrice)}"
            tvPaymentPrice.text = "R$ ${String.format(Locale.getDefault(), "%.2f", history.paymentPrice)}"
            tvCommissionPrice.text = "R$ ${String.format(Locale.getDefault(), "%.2f", history.commissionPrice)}"

            tvPaymentType.text = history.paymentMethod

            tvIdTransaction.text = history.transactionId
            tvAtk.text = history.atk

            val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
            tvDate.text = dateFormat.format(history.date)

            // Agora funciona porque a função está fora da classe
            val iconRes = history.paymentMethod.getPaymentMethodIcon()
            imageView.setImageResource(iconRes)

        }
    }
}