package br.com.ticpass.pos.core.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.sdk.AcquirerSdk
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagNearFieldCardData
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.EM1KeyType
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.PlugPagNFCAuth
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Dedicated utility for NFC sector authentication
 */
object NFCAuthenticator {
    private const val TAG = "NFCAuthenticator"
    private val plugpag = AcquirerSdk.nfc.getInstance()
    private val antenna = NFCTagReaderAntenna

    /**
     * Attempts authentication with a single key using.
     * This will leave antenna in a started state, it should be stopped manually after authentication.
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
    ): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            val deferred = CompletableDeferred<Boolean>()

            try {
                val didAuth = doAuth(
                    sectorNum = sectorNum,
                    key = key,
                    keyType = keyType,
                    timeoutMs = timeoutMs
                )

                deferred.complete(didAuth)
            }
            catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create NFC provider or execute authentication", e)
                deferred.complete(false)
            }

            deferred.await()
        } ?: false // Return null if timeout occurs
    }

    /**
     * Performs the actual authentication logic.
     */
    private fun doAuth(
        sectorNum: Int,
        key: String,
        keyType: NFCTagSectorKeyType,
        timeoutMs: Long
    ): Boolean {
        try {
            val timeoutInt = timeoutMs.toInt() / 1000
            val keyBytes = NFCUtils.hexStringToByteArray(key)
            val blockNum = sectorNum * 4

            val pagseguroKeyType = when (keyType) {
                NFCTagSectorKeyType.A -> EM1KeyType.TYPE_A
                NFCTagSectorKeyType.B -> EM1KeyType.TYPE_B
            }

            val cardData = PlugPagNFCAuth(
                PlugPagNearFieldCardData.ONLY_M,
                blockNum.toByte(),
                keyBytes,
                pagseguroKeyType
            )

            if (!antenna.start()) return false
            val result = plugpag.authNFCCardDirectly(cardData, timeoutInt)
            val isSuccess = result == PlugPag.NFC_RET_OK

            return isSuccess
        }
        catch (e: PlugPagException) {
            throw e
        }
        catch (e: Exception) {
            throw e
        }
    }
}
