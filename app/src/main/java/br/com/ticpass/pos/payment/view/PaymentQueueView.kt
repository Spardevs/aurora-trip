package br.com.ticpass.pos.payment.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.utils.toMoneyAsDouble
import java.text.NumberFormat
import java.util.Locale

/**
 * Custom view that displays the payment queue and provides operations on queue items
 */
class PaymentQueueView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val queueRecyclerView: RecyclerView
    private val queueAdapter: PaymentQueueAdapter
    
    // Callback for when a payment is canceled
    var onPaymentCanceled: ((String) -> Unit)? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_payment_queue, this, true)
        
        queueRecyclerView = view.findViewById(R.id.recycler_queue_items)
        
        // Setup RecyclerView
        queueRecyclerView.layoutManager = LinearLayoutManager(context)
        queueAdapter = PaymentQueueAdapter(emptyList()) { paymentId ->
            onPaymentCanceled?.invoke(paymentId)
        }
        queueRecyclerView.adapter = queueAdapter
    }

    /**
     * Update the queue with new items
     *
     * @param queueItems List of payment queue items to display
     */
    fun updateQueue(queueItems: List<ProcessingPaymentQueueItem>) {
        queueAdapter.updateItems(queueItems)
    }

    /**
     * Get the current number of items in the queue
     *
     * @return Number of items in the queue
     */
    fun getQueueSize(): Int {
        return queueAdapter.itemCount
    }

    /**
     * Adapter for the payment queue RecyclerView
     */
    private inner class PaymentQueueAdapter(
        private var items: List<ProcessingPaymentQueueItem>,
        private val onCancelClick: (String) -> Unit
    ) : RecyclerView.Adapter<PaymentQueueAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val paymentIdText: TextView = view.findViewById(R.id.text_payment_id)
            val paymentAmountText: TextView = view.findViewById(R.id.text_payment_amount)
            val paymentMethodText: TextView = view.findViewById(R.id.text_payment_method)
            val cancelButton: View = view.findViewById(R.id.btn_cancel_payment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_payment_queue, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.paymentIdText.text = item.id
            holder.paymentAmountText.text = formatCurrency(item.amount.toMoneyAsDouble())
            holder.paymentMethodText.text = item.method.toString()
            
            holder.cancelButton.setOnClickListener {
                onCancelClick(item.id)
            }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<ProcessingPaymentQueueItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }

        private fun formatCurrency(amount: Int): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            return formatter.format(amount)
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            return formatter.format(amount)
        }
    }
}
