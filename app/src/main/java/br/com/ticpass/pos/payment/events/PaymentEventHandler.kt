package br.com.ticpass.pos.payment.events

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiEvent
import br.com.ticpass.pos.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import com.google.android.material.snackbar.Snackbar

/**
 * Handles all payment-related events for the PaymentProcessingActivity.
 * This class extracts event handling logic to improve maintainability and testability.
 */
class PaymentEventHandler(
    private val context: Context,
    private val dialogEventTextView: TextView,
    private val onPinDisplayUpdate: () -> Unit
) {

    // For tracking PIN entry
    private val pinDigits = mutableListOf<Int>()

    /**
     * Handle payment processor events.
     * @param event The payment processor event to handle
     */
    fun handlePaymentEvent(event: ProcessingPaymentEvent) {
        // Handle PIN digit tracking
        when (event) {
            is ProcessingPaymentEvent.PIN_DIGIT_INPUT -> {
                pinDigits.add(1) // Add a digit placeholder
                onPinDisplayUpdate()
            }
            is ProcessingPaymentEvent.PIN_DIGIT_REMOVED -> {
                if (pinDigits.isNotEmpty()) {
                    pinDigits.removeAt(pinDigits.lastIndex) // Remove last digit
                    onPinDisplayUpdate()
                } else {
                    // sometimes acquirers will emit a PIN_DIGIT_REMOVED event
                    // even if no digits are present
                    clearPinDigits()
                }
            }
            is ProcessingPaymentEvent.PIN_REQUESTED -> {
                // Reset PIN digits when a new PIN is requested
                clearPinDigits()
                onPinDisplayUpdate()
            }
            is ProcessingPaymentEvent.PIN_OK -> {
                // Clear PIN digits when PIN is confirmed
                clearPinDigits()
            }
            else -> { /* Other events don't affect PIN display */ }
        }
        
        val eventMessage = getEventMessage(event)
        
        // Update dialog with the event message
        dialogEventTextView.text = eventMessage
        
        // Log the payment event
        Log.d("PaymentEventHandler", "Payment event: $eventMessage")
    }

    /**
     * Handle UI events from the ViewModel.
     * @param event The UI event to handle
     */
    fun handleUiEvent(event: PaymentProcessingUiEvent) {
        when (event) {
            // Handle navigation events
            is PaymentProcessingUiEvent.NavigateBack -> {
                if (context is android.app.Activity) {
                    context.finish()
                }
            }
            is PaymentProcessingUiEvent.NavigateToPaymentDetails -> {
                // Example: Navigate to payment details
                // val intent = Intent(context, PaymentDetailsActivity::class.java)
                // intent.putExtra("paymentId", event.paymentId)
                // context.startActivity(intent)
                Log.d("PaymentEventHandler", "Navigate to payment details: ${event.paymentId}")
            }
            
            // Handle message events
            is PaymentProcessingUiEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                Log.d("PaymentEventHandler", "Toast message: ${event.message}")
            }
            is PaymentProcessingUiEvent.ShowSnackbar -> {
                val view = (context as android.app.Activity).findViewById<View>(android.R.id.content)
                val snackbar = Snackbar.make(
                    view, 
                    event.message, 
                    Snackbar.LENGTH_LONG
                )
                event.actionLabel?.let { label ->
                    snackbar.setAction(label) {
                        // Handle snackbar action if needed
                    }
                }
                snackbar.show()
                Log.d("PaymentEventHandler", "Snackbar message: ${event.message}")
            }
            
            // Handle dialog events
            is PaymentProcessingUiEvent.ShowErrorDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("OK", null)
                    .show()
                Log.d("PaymentEventHandler", "Error dialog: ${event.title} - ${event.message}")
            }
            is PaymentProcessingUiEvent.ShowConfirmationDialog -> {
                AlertDialog.Builder(context)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("Yes") { _, _ -> 
                        // Handle confirmation
                        Log.d("PaymentEventHandler", "Confirmation dialog: Yes")
                    }
                    .setNegativeButton("No") { _, _ ->
                        Log.d("PaymentEventHandler", "Confirmation dialog: No")
                    }
                    .show()
                Log.d("PaymentEventHandler", "Confirmation dialog: ${event.title} - ${event.message}")
            }
            
            // Handle payment events
            is PaymentProcessingUiEvent.PaymentCompleted -> {
                val amountStr = event.amount.toString()
                Log.d("PaymentEventHandler", "Payment ${event.paymentId} completed: $amountStr")
            }
            is PaymentProcessingUiEvent.PaymentFailed -> {
                Log.d("PaymentEventHandler", "Payment ${event.paymentId} failed: ${event.error}")
            }
        }
    }

    /**
     * Get the current PIN digits for display purposes.
     * @return List of current PIN digits
     */
    fun getCurrentPinDigits(): List<Int> = pinDigits.toList()

    /**
     * Clear PIN digits (useful for external PIN management).
     */
    fun clearPinDigits() {
        pinDigits.clear()
    }

    /**
     * Get localized event message for a payment processor event.
     * @param event The payment processor event
     * @return Localized event message string
     */
    private fun getEventMessage(event: ProcessingPaymentEvent): String {
        return when (event) {
            is ProcessingPaymentEvent.START -> context.getString(R.string.event_start)
            is ProcessingPaymentEvent.CARD_REACH_OR_INSERT -> context.getString(R.string.event_card_reach_or_insert)
            is ProcessingPaymentEvent.APPROVAL_SUCCEEDED -> context.getString(R.string.event_approval_succeeded)
            is ProcessingPaymentEvent.APPROVAL_DECLINED -> context.getString(R.string.event_approval_declined)
            is ProcessingPaymentEvent.TRANSACTION_DONE -> context.getString(R.string.event_transaction_done)
            is ProcessingPaymentEvent.TRANSACTION_PROCESSING -> context.getString(R.string.event_transaction_processing)
            is ProcessingPaymentEvent.AUTHORIZING -> context.getString(R.string.event_authorizing)
            is ProcessingPaymentEvent.CARD_BIN_REQUESTED -> context.getString(R.string.event_card_bin_requested)
            is ProcessingPaymentEvent.CARD_BIN_OK -> context.getString(R.string.event_card_bin_ok)
            is ProcessingPaymentEvent.CARD_HOLDER_REQUESTED -> context.getString(R.string.event_card_holder_requested)
            is ProcessingPaymentEvent.CARD_HOLDER_OK -> context.getString(R.string.event_card_holder_ok)
            is ProcessingPaymentEvent.CONTACTLESS_ERROR -> context.getString(R.string.event_contactless_error)
            is ProcessingPaymentEvent.CONTACTLESS_ON_DEVICE -> context.getString(R.string.event_contactless_on_device)
            is ProcessingPaymentEvent.CVV_OK -> context.getString(R.string.event_cvv_ok)
            is ProcessingPaymentEvent.CVV_REQUESTED -> context.getString(R.string.event_cvv_requested)
            is ProcessingPaymentEvent.DOWNLOADING_TABLES -> context.getString(R.string.event_downloading_tables)
            is ProcessingPaymentEvent.SAVING_TABLES -> context.getString(R.string.event_saving_tables)
            is ProcessingPaymentEvent.USE_CHIP -> context.getString(R.string.event_use_chip)
            is ProcessingPaymentEvent.USE_MAGNETIC_STRIPE -> context.getString(R.string.event_use_magnetic_stripe)
            is ProcessingPaymentEvent.CARD_REMOVAL_REQUESTING -> context.getString(R.string.event_card_removal_requesting)
            is ProcessingPaymentEvent.KEY_INSERTED -> context.getString(R.string.event_key_inserted)
            is ProcessingPaymentEvent.ACTIVATION_SUCCEEDED -> context.getString(R.string.event_activation_succeeded)
            is ProcessingPaymentEvent.SOLVING_PENDING_ISSUES -> context.getString(R.string.event_solving_pending_issues)
            is ProcessingPaymentEvent.PIN_REQUESTED -> context.getString(R.string.event_pin_requested)
            is ProcessingPaymentEvent.CARD_INSERTED -> {
                clearPinDigits()
                context.getString(R.string.event_card_inserted)
            }
            is ProcessingPaymentEvent.PIN_DIGIT_INPUT -> {
                // For PIN input, show the PIN with asterisks
                val pinDisplay = PaymentUIUtils.formatPinDisplay(pinDigits)
                context.getString(R.string.event_pin_digit_input) + " $pinDisplay"
            }
            is ProcessingPaymentEvent.PIN_DIGIT_REMOVED -> {
                // For PIN removal, show the updated PIN with asterisks
                val pinDisplay = PaymentUIUtils.formatPinDisplay(pinDigits)
                context.getString(R.string.event_pin_digit_removed) + " $pinDisplay"
            }
            is ProcessingPaymentEvent.CARD_REMOVAL_SUCCEEDED -> {
                clearPinDigits()
                context.getString(R.string.event_card_removal_succeeded)
            }
            is ProcessingPaymentEvent.PIN_OK -> context.getString(R.string.event_pin_ok)
            is ProcessingPaymentEvent.GENERIC_SUCCESS -> context.getString(R.string.event_generic_success)
            is ProcessingPaymentEvent.GENERIC_ERROR -> context.getString(R.string.event_generic_error)
            ProcessingPaymentEvent.CANCELLED -> context.getString(R.string.event_cancelled)
        }
    }
}
