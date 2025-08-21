package br.com.ticpass.pos.nfc.utils

import android.content.Context
import android.util.Log
import br.com.ticpass.Constants.NFC_KEY_TYPE_A
import br.com.ticpass.Constants.NFC_KEY_TYPE_B
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.queue.processors.nfc.processors.models.NFCProcessorType

/**
 * Utility functions for nfc processing UI operations.
 * This class extracts common utility functions to improve code reusability and maintainability.
 */
object NFCUIUtils {
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
     * Create nfc data for enqueueing.
     * @param method NFC method
     * @param isTransactionlessEnabled Whether transactionless mode is enabled
     * @param amount Optional custom amount (if null, generates random amount)
     * @param commission Optional commission amount (defaults to 0)
     * @return NFCData object containing all nfc information
     */
    fun createNFCData(
        filePath: String?,
        processorType: NFCProcessorType
    ): NFCData {
        return NFCData(
            filePath = filePath ?: "",
            processorType = processorType
        )
    }

    /**
     * Data class representing nfc information for enqueueing.
     */
    data class NFCData(
        val filePath: String,
        val processorType: NFCProcessorType
    )

    /**
     * Get NFC keys from constants
     * @return map of NFC Keys
     */
    fun getNFCKeys(): Map<NFCTagSectorKeyType, String> {
        return mapOf(
            NFCTagSectorKeyType.A to NFC_KEY_TYPE_A,
            NFCTagSectorKeyType.B to NFC_KEY_TYPE_B
        )
    }
}
