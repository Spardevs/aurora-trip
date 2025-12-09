package br.com.ticpass.pos.presentation.printing.view

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
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingQueueItem

/**
 * Custom view that displays the printing queue and provides operations on queue items
 */
class PrintingQueueView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val queueRecyclerView: RecyclerView
    private val queueAdapter: PrintingQueueAdapter
    
    // Callback for when a printing is canceled
    var onPrintingCanceled: ((String) -> Unit)? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_printing_queue, this, true)
        
        queueRecyclerView = view.findViewById(R.id.recycler_queue_items)
        
        // Setup RecyclerView
        queueRecyclerView.layoutManager = LinearLayoutManager(context)
        queueAdapter = PrintingQueueAdapter(emptyList()) { printingId ->
            onPrintingCanceled?.invoke(printingId)
        }
        queueRecyclerView.adapter = queueAdapter
    }

    /**
     * Update the queue with new items
     *
     * @param queueItems List of printing queue items to display
     */
    fun updateQueue(queueItems: List<PrintingQueueItem>) {
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
     * Adapter for the printing queue RecyclerView
     */
    private inner class PrintingQueueAdapter(
        private var items: List<PrintingQueueItem>,
        private val onCancelClick: (String) -> Unit
    ) : RecyclerView.Adapter<PrintingQueueAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val printingIdText: TextView = view.findViewById(R.id.text_printing_id)
            val printingProcessorText: TextView = view.findViewById(R.id.text_printing_processor_type)
            val cancelButton: View = view.findViewById(R.id.btn_cancel_printing)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_printing_queue, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.printingIdText.text = item.id
            holder.printingProcessorText.text = item.processorType.toString()
            
            holder.cancelButton.setOnClickListener {
                onCancelClick(item.id)
            }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<PrintingQueueItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }
    }
}
