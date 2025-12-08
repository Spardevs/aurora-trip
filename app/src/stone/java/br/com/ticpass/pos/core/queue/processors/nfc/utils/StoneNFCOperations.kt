package br.com.ticpass.pos.core.queue.processors.nfc.utils

import android.util.Log
import br.com.stone.posandroid.hal.api.mifare.MifareKeyType
import br.com.stone.posandroid.providers.PosMifareProvider
import br.com.ticpass.pos.core.nfc.models.NFCTagDetectionResult
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.core.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCUtils.byteArrayToHexString
import br.com.ticpass.pos.core.sdk.factory.AcquirerNFCProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import stone.application.enums.ErrorsEnum
import stone.application.interfaces.StoneCallbackInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stone implementation of NFCOperations.
 * Provides NFC tag detection, reading, and writing using Stone's PosMifareProvider.
 */
@Singleton
class StoneNFCOperations @Inject constructor(
    private val nfcProviderFactory: AcquirerNFCProvider
) : NFCOperations {

    companion object {
        private const val TAG = "StoneNFCOperations"
    }

    // ==================== Tag Detection ====================

    override suspend fun detectTag(timeoutMs: Long): NFCTagDetectionResult? {
        return withTimeoutOrNull(timeoutMs) {
            val deferred = CompletableDeferred<NFCTagDetectionResult?>()
            val mifareProvider = nfcProviderFactory()

            try {
                Log.d(TAG, "🔍 Starting card detection...")

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
                        } catch (e: NFCException) {
                            Log.e(TAG, "❌ Error retrieving card UUID ${e.error}")
                            deferred.complete(null)
                        } catch (e: AcquirerNFCException) {
                            Log.e(TAG, "❌ Error retrieving card UUID ${e.event}")
                            deferred.complete(null)
                        } catch (e: Exception) {
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
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error handling card detection failure", e)
                            cleanupProvider(mifareProvider)
                            deferred.complete(null)
                        }
                    }
                }

                mifareProvider.execute()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create NFC provider or execute detection", e)
                cleanupProvider(mifareProvider)
                deferred.complete(null)
            }

            deferred.await()
        }
    }

    override fun abortDetection() {
        // Stone SDK doesn't have a global abort - each provider manages its own lifecycle
        Log.d(TAG, "🛑 Abort detection requested")
    }

    private fun cleanupProvider(provider: PosMifareProvider?) {
        try {
            provider?.powerOff()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Warning: Failed to power off NFC provider", e)
        }
    }

    // ==================== Antenna Control ====================

    override fun startAntenna(): Boolean {
        // Stone SDK manages antenna automatically via PosMifareProvider
        return true
    }

    override fun stopAntenna(): Boolean {
        // Stone SDK manages antenna automatically via PosMifareProvider
        return true
    }

    // ==================== Authentication ====================

    private suspend fun authenticateWithKey(
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

                nfcProvider.connectionCallback = object : StoneCallbackInterface {
                    override fun onSuccess() {
                        try {
                            val isTypeA = keyType == NFCTagSectorKeyType.A
                            val stoneKeyType = if (isTypeA) MifareKeyType.TypeA else MifareKeyType.TypeB
                            nfcProvider.authenticateSector(stoneKeyType, keyBytes, sectorNum.toByte())
                            Log.d(TAG, "✅ Authentication succeeded: key=$key, sector=$sectorNum, type=$stoneKeyType")
                            deferred.complete(nfcProvider)
                        } catch (e: PosMifareProvider.MifareException) {
                            Log.d(TAG, "❌ Authentication failed: key=$key, sector=$sectorNum, type=$keyType, error=${e.errorEnum}")
                            cleanupProvider(nfcProvider)
                            deferred.complete(null)
                        } catch (e: Exception) {
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
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error handling NFC connection failure", e)
                            cleanupProvider(nfcProvider)
                            deferred.complete(null)
                        }
                    }
                }

                nfcProvider.execute()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create NFC provider or execute authentication", e)
                cleanupProvider(nfcProvider)
                deferred.complete(null)
            }

            deferred.await()
        }
    }

    // ==================== Block Reading ====================

    override suspend fun readBlock(
        sectorNum: Int,
        blockNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): ByteArray? {
        Log.d(TAG, "📖 Reading sector $sectorNum block $blockNum")

        // Try Key A first
        sectorKeys.typeA?.let { keyA ->
            Log.d(TAG, "🔑 Trying Key A: $keyA")
            val result = tryReadWithKey(sectorNum, blockNum, keyA, NFCTagSectorKeyType.A)
            if (result != null) {
                Log.d(TAG, "✅ Read successful with Key A")
                return result
            }
        }

        // Try Key B if Key A failed
        sectorKeys.typeB?.let { keyB ->
            Log.d(TAG, "🔑 Trying Key B: $keyB")
            val result = tryReadWithKey(sectorNum, blockNum, keyB, NFCTagSectorKeyType.B)
            if (result != null) {
                Log.d(TAG, "✅ Read successful with Key B")
                return result
            }
        }

        Log.w(TAG, "❌ Failed to read sector $sectorNum block $blockNum with both keys")
        return null
    }

    override suspend fun readSectorTrailer(
        sectorNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): ByteArray? {
        val trailerBlockNum = if (sectorNum < 32) 3 else 15
        return readBlock(sectorNum, trailerBlockNum, sectorKeys)
    }

    private suspend fun tryReadWithKey(
        sectorNum: Int,
        blockNum: Int,
        keyHex: String,
        keyType: NFCTagSectorKeyType
    ): ByteArray? {
        return try {
            val provider = authenticateWithKey(sectorNum, keyHex, keyType, 1000L)

            if (provider == null) {
                Log.d(TAG, "⚠️ Authentication failed with Key $keyType")
                return null
            }

            try {
                val blockData = ByteArray(16)
                provider.readBlock(sectorNum.toByte(), blockNum.toByte(), blockData)
                Log.d(TAG, "📖 Read block data: ${byteArrayToHexString(blockData)}")
                blockData
            } catch (e: PosMifareProvider.MifareException) {
                Log.d(TAG, "⚠️ Read failed with Key $keyType: ${e.errorEnum}")
                null
            } finally {
                provider.powerOff()
            }
        } catch (e: PosMifareProvider.MifareException) {
            Log.d(TAG, "⚠️ Read failed with Key $keyType: ${e.errorEnum}")
            null
        } catch (e: Exception) {
            Log.d(TAG, "⚠️ Read failed with Key $keyType: ${e.message}")
            null
        }
    }

    // ==================== Block Writing ====================

    override suspend fun writeBlock(
        sectorNum: Int,
        blockNum: Int,
        data: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        Log.d(TAG, "📝 Writing sector $sectorNum block $blockNum")

        // Try Key A first
        sectorKeys.typeA?.let { keyA ->
            Log.d(TAG, "🔑 Trying Key A: $keyA")
            val isSuccess = tryWriteWithKey(sectorNum, blockNum, data, keyA, NFCTagSectorKeyType.A)
            if (isSuccess) {
                Log.d(TAG, "✅ Write successful with Key A")
                return true
            }
        }

        // Try Key B if Key A failed
        sectorKeys.typeB?.let { keyB ->
            Log.d(TAG, "🔑 Trying Key B: $keyB")
            val isSuccess = tryWriteWithKey(sectorNum, blockNum, data, keyB, NFCTagSectorKeyType.B)
            if (isSuccess) {
                Log.d(TAG, "✅ Write successful with Key B")
                return true
            }
        }

        Log.w(TAG, "❌ Failed to write sector $sectorNum block $blockNum with both keys")
        return false
    }

    override suspend fun writeSectorTrailer(
        sectorNum: Int,
        newTrailerData: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        val trailerBlockNum = if (sectorNum < 32) 3 else 15
        return writeBlock(sectorNum, trailerBlockNum, newTrailerData, sectorKeys)
    }

    override suspend fun clearDataBlock(
        sectorNum: Int,
        blockNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        val maxDataBlock = if (sectorNum < 32) 2 else 14
        if (blockNum > maxDataBlock) {
            Log.w(TAG, "⚠️ Refusing to clear sector trailer block $blockNum in sector $sectorNum")
            return false
        }

        val emptyData = NFCUtils.createEmptyBlockData()
        Log.d(TAG, "🧹 Clearing sector $sectorNum block $blockNum")
        return writeBlock(sectorNum, blockNum, emptyData, sectorKeys)
    }

    private suspend fun tryWriteWithKey(
        sectorNum: Int,
        blockNum: Int,
        data: ByteArray,
        keyHex: String,
        keyType: NFCTagSectorKeyType
    ): Boolean {
        return try {
            val provider = authenticateWithKey(sectorNum, keyHex, keyType, 1000L)

            if (provider == null) {
                Log.d(TAG, "⚠️ Authentication failed with Key $keyType")
                return false
            }

            try {
                provider.writeBlock(sectorNum.toByte(), blockNum.toByte(), data)
                Log.d(TAG, "✏️ Write successful with Key $keyType")
                true
            } catch (e: PosMifareProvider.MifareException) {
                Log.d(TAG, "⚠️ Write failed with Key $keyType: ${e.errorEnum}")
                false
            } finally {
                provider.powerOff()
            }
        } catch (e: PosMifareProvider.MifareException) {
            Log.d(TAG, "⚠️ Write failed with Key $keyType: ${e.errorEnum}")
            false
        } catch (e: Exception) {
            Log.d(TAG, "⚠️ Write failed with Key $keyType: ${e.message}")
            false
        }
    }
}
