package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.stone.posandroid.hal.api.mifare.MifareKeyType
import br.com.stone.posandroid.providers.PosMifareProvider
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.sdk.AcquirerSdk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import stone.application.enums.ErrorsEnum
import stone.application.interfaces.StoneCallbackInterface

/**
 * Dedicated utility for NFC sector authentication
 */
object NFCAuthenticator {
    private val nfcProviderFactory = AcquirerSdk.nfc.getInstance()
    private const val TAG = "NFCAuthenticator"

    /**
     * Attempts authentication with a single key using a fresh provider instance
     * 
     * @param sectorNum The sector number to authenticate
     * @param key The hex string key to use
     * @param keyType The key type (A or B)
     * @param timeoutMs Timeout for this authentication attempt
     * @return PosMifareProvider? - authenticated provider instance, or null if authentication failed
     */
    suspend fun authenticateWithKey(
        sectorNum: Int,
        key: String,
        keyType: NFCTagSectorKeyType,
        timeoutMs: Long
    ): PosMifareProvider? {
        return withTimeoutOrNull(timeoutMs) {
            val deferred = CompletableDeferred<PosMifareProvider?>()
            val nfcProvider = nfcProviderFactory()
            
            try {
                val keyBytes = NFCUtils.hexStringToByteArray(key)
                if (keyBytes.size != 6) {
                    Log.w(TAG, "Invalid key length for $key: ${keyBytes.size} bytes")
                    cleanupProvider(nfcProvider)
                    deferred.complete(null)
                    return@withTimeoutOrNull deferred.await()
                }
                
                // Set up callback for this specific authentication attempt
                nfcProvider.connectionCallback = connectionCallback(
                    deferred,
                    nfcProvider,
                    sectorNum,
                    key,
                    keyType,
                    keyBytes
                )
                
                // Execute the NFC operation
                nfcProvider.execute()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create NFC provider or execute authentication", e)
                cleanupProvider(nfcProvider)
                deferred.complete(null)
            }
            
            deferred.await()
        } // Return null if timeout occurs
    }

    private fun connectionCallback(
        deferred: CompletableDeferred<PosMifareProvider?>,
        nfcProvider: PosMifareProvider,
        sectorNum: Int,
        key: String,
        keyType: NFCTagSectorKeyType,
        keyBytes: ByteArray
    ): StoneCallbackInterface {
        return object : StoneCallbackInterface {
            override fun onSuccess() {
                try {
                    val isTypeA = keyType == NFCTagSectorKeyType.A
                    val stoneKeyType = if(isTypeA) MifareKeyType.TypeA else MifareKeyType.TypeB
                    nfcProvider.authenticateSector(stoneKeyType, keyBytes, sectorNum.toByte())
                    Log.d(TAG, "✅ Authentication succeeded: key=$key, sector=$sectorNum, type=$stoneKeyType")
                    deferred.complete(nfcProvider)
                }
                catch (e: PosMifareProvider.MifareException) {
                    Log.d(TAG, "❌ Authentication failed: key=$key, sector=$sectorNum, type=$keyType, error=${e.errorEnum}")
                    cleanupProvider(nfcProvider)
                    deferred.complete(null)
                }
                catch (e: Exception) {
                    Log.e(TAG, "❌ Unexpected error during authentication: key=$key, sector=$sectorNum, type=$keyType", e)
                    cleanupProvider(nfcProvider)
                    deferred.complete(null)
                }
            }

            override fun onError() {
                try {
                    val error = nfcProvider.listOfErrors?.lastOrNull() ?: ErrorsEnum.UNKNOWN_ERROR
                    Log.d(TAG, "❌ NFC connection error: key=$key, sector=$sectorNum, type=$keyType, error=$error")
                    cleanupProvider(nfcProvider)
                    deferred.complete(null)
                }
                catch (e: PosMifareProvider.MifareException) {
                    Log.e(TAG, "❌ Error handling NFC connection failure ${e.errorEnum}")
                    cleanupProvider(nfcProvider)
                    deferred.complete(null)
                }
                catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling NFC connection failure", e)
                    cleanupProvider(nfcProvider)
                    deferred.complete(null)
                }
            }
        }
    }
    
    /**
     * Safely cleanup the NFC provider
     */
    private fun cleanupProvider(provider: PosMifareProvider?) {
        try {
            provider?.powerOff()
        }
        catch (e: PosMifareProvider.MifareException) {
            Log.w(TAG, "Warning: Failed to power off NFC provider: ${e.errorEnum}")
        }
        catch (e: Exception) {
            Log.w(TAG, "Warning: Failed to power off NFC provider", e)
        }
    }
}
