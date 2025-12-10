package br.com.ticpass.pos.presentation.refund.view

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
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem

/**
 * Custom view that displays the refund queue and provides operations on queue items
 */
class RefundQueueView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val queueRecyclerView: RecyclerView
    private val queueAdapter: RefundQueueAdapter
    
    // Callback for when a refund is canceled
    var onRefundCanceled: ((String) -> Unit)? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_refund_queue, this, true)
        
        queueRecyclerView = view.findViewById(R.id.recycler_queue_items)
        
        // Setup RecyclerView
        queueRecyclerView.layoutManager = LinearLayoutManager(context)
        queueAdapter = RefundQueueAdapter(emptyList()) { refundId ->
            onRefundCanceled?.invoke(refundId)
        }
        queueRecyclerView.adapter = queueAdapter
    }

    /**
     * Update the queue with new items
     *
     * @param queueItems List of refund queue items to display
     */
    fun updateQueue(queueItems: List<RefundQueueItem>) {
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
     * Adapter for the refund queue RecyclerView
     */
    private inner class RefundQueueAdapter(
        private var items: List<RefundQueueItem>,
        private val onCancelClick: (String) -> Unit
    ) : RecyclerView.Adapter<RefundQueueAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val refundIdText: TextView = view.findViewById(R.id.text_refund_id)
            val refundProcessorText: TextView = view.findViewById(R.id.text_refund_processor_type)
            val cancelButton: View = view.findViewById(R.id.btn_cancel_refund)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_refund_queue, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.refundIdText.text = item.id
            holder.refundProcessorText.text = item.processorType.toString()
            
            holder.cancelButton.setOnClickListener {
                onCancelClick(item.id)
            }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<RefundQueueItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }
    }
}
