package br.com.ticpass.pos.presentation.nfc.view

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
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem

/**
 * Custom view that displays the nfc queue and provides operations on queue items
 */
class NFCQueueView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val queueRecyclerView: RecyclerView
    private val queueAdapter: NFCQueueAdapter
    
    // Callback for when a nfc is canceled
    var onNFCCanceled: ((String) -> Unit)? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_nfc_queue, this, true)
        
        queueRecyclerView = view.findViewById(R.id.recycler_queue_items)
        
        // Setup RecyclerView
        queueRecyclerView.layoutManager = LinearLayoutManager(context)
        queueAdapter = NFCQueueAdapter(emptyList()) { nfcId ->
            onNFCCanceled?.invoke(nfcId)
        }
        queueRecyclerView.adapter = queueAdapter
    }

    /**
     * Update the queue with new items
     *
     * @param queueItems List of nfc queue items to display
     */
    fun updateQueue(queueItems: List<NFCQueueItem>) {
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
     * Adapter for the nfc queue RecyclerView
     */
    private inner class NFCQueueAdapter(
        private var items: List<NFCQueueItem>,
        private val onCancelClick: (String) -> Unit
    ) : RecyclerView.Adapter<NFCQueueAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nfcIdText: TextView = view.findViewById(R.id.text_nfc_id)
            val nfcProcessorText: TextView = view.findViewById(R.id.text_nfc_processor_type)
            val cancelButton: View = view.findViewById(R.id.btn_cancel_nfc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_nfc_queue, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.nfcIdText.text = item.id
            holder.nfcProcessorText.text = item.processorType.toString()
            
            holder.cancelButton.setOnClickListener {
                onCancelClick(item.id)
            }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<NFCQueueItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }
    }
}
