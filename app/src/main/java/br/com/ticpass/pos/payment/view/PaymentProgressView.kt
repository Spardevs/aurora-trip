package br.com.ticpass.pos.payment.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import br.com.ticpass.pos.R
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingErrorEventResourceMapper

/**
 * Custom view that displays payment processing progress
 * Encapsulates progress text, progress bar, and current event display
 */
class PaymentProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val progressTextView: TextView
    private val progressBar: ProgressBar
    private val currentEventTextView: TextView

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_payment_progress, this, true)
        
        progressTextView = view.findViewById(R.id.text_processing_progress)
        progressBar = view.findViewById(R.id.progress_bar)
        currentEventTextView = view.findViewById(R.id.text_current_event)
    }

    /**
     * Update the progress display
     *
     * @param current Current item index being processed
     * @param total Total number of items to process
     */
    fun updateProgress(current: Int, total: Int) {
        progressTextView.text = context.getString(R.string.payment_progress, current, total)
        progressTextView.setTextColor(context.getColor(android.R.color.black))
        progressBar.progress = current
        progressBar.max = total
    }

    /**
     * Display an error in the progress view
     * @param error The error event to display
     */
    fun displayError(error: ProcessingErrorEvent) {
        // Use the mapper to get the correct string resource ID
        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(error)
        
        val errorMessage = context.getString(resourceId)
        
        progressTextView.text = errorMessage
        progressTextView.setTextColor(context.getColor(android.R.color.holo_red_dark))
    }

    /**
     * Update the current event text
     *
     * @param eventText Text describing the current event
     */
    fun updateCurrentEvent(eventText: String) {
        currentEventTextView.text = eventText
    }

    /**
     * Reset the progress view to its initial state
     */
    fun reset() {
        progressTextView.text = context.getString(R.string.payment_progress, 0, 0)
        progressTextView.setTextColor(context.getColor(android.R.color.black))
        progressBar.progress = 0
        progressBar.max = 0
        currentEventTextView.text = ""
    }
}
