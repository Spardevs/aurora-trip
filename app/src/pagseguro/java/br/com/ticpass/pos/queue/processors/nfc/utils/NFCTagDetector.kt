package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.nfc.models.NFCTagDetectionResult
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCUtils.byteArrayToHexString
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagNearFieldCardData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Utility for NFC tag detection operations.
 * Provides card detection functionality with UUID retrieval.
 */
object NFCTagDetector {
    private const val TAG = "NFCTagDetector"
    private val plugpag = AcquirerSdk.nfc.getInstance()
    private val antenna = NFCTagReaderAntenna
    
    /**
     * Detects an NFC card and retrieves its UUID.
     * This is equivalent to the detectTag method from the Java example.
     * 
     * @param timeoutMs Timeout for card detection (default 10 seconds)
     * @return NFCTagDetectionResult? - detection result with UUID and provider, or null if failed
     */
    suspend fun detectTag(timeoutMs: Long = 10000L): NFCTagDetectionResult? {
        return withTimeoutOrNull(timeoutMs) {
            val deferred = CompletableDeferred<NFCTagDetectionResult?>()

            Log.d(TAG, "🔍 Starting card detection...")

            try {
                val cardUUID = doDetect(timeoutMs)
                val cardUUIDString = byteArrayToHexString(cardUUID)
                Log.d(TAG, "✅ Card detected: $cardUUIDString")

                val result = NFCTagDetectionResult(
                    cardUUID = cardUUID,
                    cardUUIDString = cardUUIDString,
                )
                deferred.complete(result)
            }
            catch (e: NFCException) {
                Log.e(TAG, "❌ Error retrieving card UUID ${e.error}")
                deferred.complete(null)
            }
            catch (e: AcquirerNFCException) {
                Log.e(TAG, "❌ Error retrieving card UUID ${e.event}")
                deferred.complete(null)
            }
            catch (e: Exception) {
                Log.e(TAG, "❌ Error retrieving card UUID", e)
                abort()
                deferred.complete(null)
            }
            
            deferred.await()
        } // Return null if timeout occurs
    }
    
    /**
     * Cancels ongoing card detection.
     * This is equivalent to the cancelDetectCard method from the Java example.
     */
    fun abort() {
        try {
            antenna.stop()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Warning: Failed to cancel detection", e)
        }
    }

    /**
     * Performs the card detection action.
     */
    private fun doDetect(timeoutMs: Long): ByteArray {
        return try {
            if (!antenna.start()) throw NFCException(
                ProcessingErrorEvent.NFC_TAG_REACH_TIMEOUT
            )

            val cardSerial = detectDirectly(timeoutMs) ?: throw NFCException(
                ProcessingErrorEvent.NFC_TAG_REACH_TIMEOUT
            )
            abort()

            cardSerial
        }
        catch (e: PlugPagException) {
            plugpag.stopNFCCardDirectly()
            throw e
        }
    }

    /**
     * Detects the NFC card and retrieves its serial number.
     */
    private fun detectDirectly(timeoutMs: Long): ByteArray? {
        return try {
            // ms to seconds (int)
            val timeoutInt = timeoutMs.toInt() / 1000
            val detect = plugpag.detectNfcCardDirectly(
                PlugPagNearFieldCardData.ONLY_M,
                timeoutInt
            )

            if (detect.result == PlugPag.NFC_RET_OK && detect.serialNumber != null) {
                detect.serialNumber!!
            } else {
                abort()
                null
            }
        } catch (e: Exception) {
            abort()
            null
        }
    }
}
