package br.com.ticpass.pos.payment.utils

import android.content.Context
import android.util.Log
import android.widget.CheckBox
import br.com.ticpass.pos.R
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType
import br.com.ticpass.pos.queue.processors.payment.processors.utils.PaymentMethodProcessorMapper

/**
 * Utility functions for payment processing UI operations.
 * This class extracts common utility functions to improve code reusability and maintainability.
 */
object PaymentUIUtils {

    /**
     * Generate a random payment amount for demonstration purposes.
     * @return Random amount between R$10.00 and R$200.00 (in cents)
     */
    fun generateRandomPaymentAmount(): Int {
        return (1000..20000).random()
    }

    /**
     * Determine the processor type based on payment method and transactionless mode.
     * @param method The payment method
     * @param isTransactionlessEnabled Whether transactionless mode is enabled
     * @return The appropriate processor type
     */
    fun determineProcessorType(
        method: SystemPaymentMethod,
        isTransactionlessEnabled: Boolean
    ): PaymentProcessorType {
        return if (isTransactionlessEnabled) {
            PaymentProcessorType.TRANSACTIONLESS
        } else {
            PaymentMethodProcessorMapper.getProcessorTypeForMethod(method)
        }
    }

    /**
     * Get error message string from ProcessingErrorEvent.
     * @param context Context for string resource access
     * @param error The processing error event
     * @return Localized error message string
     */
    fun getErrorMessage(context: Context, error: ProcessingErrorEvent): String {
        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(error)
        return context.getString(resourceId)
    }

    /**
     * Log error message with consistent formatting.
     * @param tag Log tag
     * @param error The processing error event
     * @param context Context for string resource access
     */
    fun logError(tag: String, error: ProcessingErrorEvent, context: Context) {
        val errorMessage = getErrorMessage(context, error)
        Log.d(tag, "Displaying error: $errorMessage")
    }

    /**
     * Get hardcoded PIX key for demonstration purposes.
     * NOTE: In a real-world application, this should be fetched from a database
     * or user preferences instead of being hardcoded.
     * @return Hardcoded PIX key
     */
    fun getHardcodedPixKey(): String {
        return "payfor@stupid.codes"
    }

    /**
     * Create payment data for enqueueing.
     * @param method Payment method
     * @param isTransactionlessEnabled Whether transactionless mode is enabled
     * @param amount Optional custom amount (if null, generates random amount)
     * @param commission Optional commission amount (defaults to 0)
     * @return PaymentData object containing all payment information
     */
    fun createPaymentData(
        method: SystemPaymentMethod,
        isTransactionlessEnabled: Boolean,
        amount: Int? = null,
        commission: Int = 0
    ): PaymentData {
        return PaymentData(
            amount = amount ?: generateRandomPaymentAmount(),
            commission = commission,
            method = method,
            processorType = determineProcessorType(method, isTransactionlessEnabled)
        )
    }

    /**
     * Data class representing payment information for enqueueing.
     */
    data class PaymentData(
        val amount: Int,
        val commission: Int,
        val method: SystemPaymentMethod,
        val processorType: PaymentProcessorType
    )

    /**
     * Check if transactionless mode is enabled from a checkbox.
     * @param transactionlessCheckbox The checkbox to check
     * @return True if transactionless mode is enabled
     */
    fun isTransactionlessModeEnabled(transactionlessCheckbox: CheckBox?): Boolean {
        return transactionlessCheckbox?.isChecked ?: false
    }

    /**
     * Format PIN digits for display as asterisks.
     * @param pinDigits List of PIN digits
     * @return String of asterisks representing the PIN
     */
    fun formatPinDisplay(pinDigits: List<Int>): String {
        return "*".repeat(pinDigits.size)
    }

    /**
     * Generate PIN display message for UI.
     * @param pinDigits List of current PIN digits
     * @return Formatted PIN message for display
     */
    fun generatePinDisplayMessage(pinDigits: List<Int>): String {
        val pinDisplay = formatPinDisplay(pinDigits)
        return "PIN: $pinDisplay"
    }
}
