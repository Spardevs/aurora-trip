package br.com.ticpass.pos.refund.utils

import android.content.Context
import android.util.Log
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.processors.refund.processors.models.RefundProcessorType

/**
 * Utility functions for refund processing UI operations.
 * This class extracts common utility functions to improve code reusability and maintainability.
 */
object RefundUIUtils {
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
     * Create refund data for enqueueing.
     * @param atk Authentication token for the refund
     * @param txId Optional transaction ID associated with the refund
     * @param processorType Type of refund processor being used
     * @return RefundData object containing all refund information
     */
    fun createRefundData(
        atk: String,
        txId: String?,
        isQRCode: Boolean?,
        processorType: RefundProcessorType
    ): RefundData {
        return RefundData(
            atk = atk,
            txId = txId ?: "",
            isQRCode = isQRCode ?: false,
            processorType = processorType
        )
    }

    /**
     * Data class representing refund information for enqueueing.
     */
    data class RefundData(
        val atk: String,
        val txId: String,
        val isQRCode: Boolean,
        val processorType: RefundProcessorType
    )
}
