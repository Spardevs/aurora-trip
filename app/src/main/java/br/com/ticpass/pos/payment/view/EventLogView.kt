package br.com.ticpass.pos.payment.view

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import br.com.ticpass.pos.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom view that displays a log of payment processing events
 * Provides methods to add different types of events with timestamps
 */
class EventLogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val eventLogTextView: TextView
    private val eventLog = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_event_log, this, true)
        eventLogTextView = view.findViewById(R.id.text_event_log)
        eventLogTextView.movementMethod = ScrollingMovementMethod()
    }

    /**
     * Add a message to the event log with current timestamp
     *
     * @param message The message to add to the log
     */
    fun addMessage(message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $message\n"
        
        eventLog.append(logEntry)
        eventLogTextView.text = eventLog.toString()
        
        // Auto-scroll to bottom
        eventLogTextView.post {
            val scrollAmount = eventLogTextView.layout.getLineTop(eventLogTextView.lineCount) - eventLogTextView.height
            if (scrollAmount > 0) {
                eventLogTextView.scrollTo(0, scrollAmount)
            } else {
                eventLogTextView.scrollTo(0, 0)
            }
        }
    }

    /**
     * Add an error message to the event log
     *
     * @param errorMessage The error message to add
     */
    fun addErrorMessage(errorMessage: String) {
        addMessage("ERROR: $errorMessage")
    }

    /**
     * Add a success message to the event log
     *
     * @param successMessage The success message to add
     */
    fun addSuccessMessage(successMessage: String) {
        addMessage("SUCCESS: $successMessage")
    }

    /**
     * Add a payment event message to the log
     *
     * @param eventTitle The title of the event
     * @param details Additional details about the event
     */
    fun addPaymentEvent(eventTitle: String, details: String? = null) {
        val message = if (details != null) {
            "$eventTitle - $details"
        } else {
            eventTitle
        }
        addMessage(message)
    }

    /**
     * Clear the event log
     */
    fun clearLog() {
        eventLog.clear()
        eventLogTextView.text = ""
    }
}
