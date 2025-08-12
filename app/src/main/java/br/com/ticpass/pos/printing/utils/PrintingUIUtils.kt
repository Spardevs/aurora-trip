package br.com.ticpass.pos.printing.utils

import android.content.Context
import android.util.Log
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType

/**
 * Utility functions for printing processing UI operations.
 * This class extracts common utility functions to improve code reusability and maintainability.
 */
object PrintingUIUtils {
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
     * Create printing data for enqueueing.
     * @param method Printing method
     * @param isTransactionlessEnabled Whether transactionless mode is enabled
     * @param amount Optional custom amount (if null, generates random amount)
     * @param commission Optional commission amount (defaults to 0)
     * @return PrintingData object containing all printing information
     */
    fun createPrintingData(
        filePath: String?,
        processorType: PrintingProcessorType
    ): PrintingData {
        return PrintingData(
            filePath = filePath ?: "",
            processorType = processorType
        )
    }

    /**
     * Data class representing printing information for enqueueing.
     */
    data class PrintingData(
        val filePath: String,
        val processorType: PrintingProcessorType
    )
}
