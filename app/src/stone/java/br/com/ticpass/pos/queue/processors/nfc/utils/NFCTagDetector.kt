package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.stone.posandroid.providers.PosMifareProvider
import br.com.ticpass.pos.nfc.models.NFCTagDetectionResult
import br.com.ticpass.pos.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCUtils.byteArrayToHexString
import br.com.ticpass.pos.sdk.AcquirerSdk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import stone.application.interfaces.StoneCallbackInterface

/**
 * Utility for NFC tag detection operations.
 * Provides card detection functionality with UUID retrieval.
 */
object NFCTagDetector {
    private val nfcProviderFactory = AcquirerSdk.nfc.getInstance()
    private const val TAG = "NFCTagDetector"
    
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
            val mifareProvider = nfcProviderFactory()
            
            try {
                Log.d(TAG, "🔍 Starting card detection...")
                
                // Set up callback for card detection
                mifareProvider.connectionCallback = object : StoneCallbackInterface {
                    override fun onSuccess() {
                        try {
                            val cardUUID = mifareProvider.cardUUID
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
                            cleanupProvider(mifareProvider)
                            deferred.complete(null)
                        }
                    }
                    
                    override fun onError() {
                        try {
                            val errors = mifareProvider.listOfErrors
                            Log.d(TAG, "❌ Card detection error: $errors")
                            cleanupProvider(mifareProvider)
                            deferred.complete(null)
                        }
                        catch (e: Exception) {
                            Log.e(TAG, "❌ Error handling card detection failure", e)
                            cleanupProvider(mifareProvider)
                            deferred.complete(null)
                        }
                    }
                }
                
                // Execute the card detection
                mifareProvider.execute()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create NFC provider or execute detection", e)
                cleanupProvider(mifareProvider)
                deferred.complete(null)
            }
            
            deferred.await()
        } // Return null if timeout occurs
    }
    
    /**
     * Cancels ongoing card detection.
     *
     * @param provider The PosMifareProvider to cancel detection on
     */
    fun cancelDetection(provider: PosMifareProvider?) {
        try {
            if (provider != null) {
                Log.d(TAG, "🛑 Cancelling card detection...")
                provider.cancelDetection()
                cleanupProvider(provider)
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Warning: Failed to cancel detection", e)
        }
    }
    
    /**
     * Safely cleanup the NFC provider.
     * This ensures proper resource management.
     */
    private fun cleanupProvider(provider: PosMifareProvider?) {
        try {
            provider?.powerOff()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Warning: Failed to power off NFC provider", e)
        }
    }
}
