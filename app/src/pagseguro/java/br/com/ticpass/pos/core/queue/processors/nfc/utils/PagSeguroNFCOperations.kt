package br.com.ticpass.pos.core.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.core.nfc.models.NFCTagDetectionResult
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.core.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCUtils.byteArrayToHexString
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagNearFieldCardData
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.EM1KeyType
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.PlugPagNFCAuth
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.PlugPagSimpleNFCData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PagSeguro implementation of NFCOperations.
 * Provides NFC tag detection, reading, and writing using PlugPag SDK.
 */
@Singleton
class PagSeguroNFCOperations @Inject constructor(
    private val plugpag: PlugPag
) : NFCOperations {

    companion object {
        private const val TAG = "PagSeguroNFCOperations"
    }

    // ==================== Tag Detection ====================

    override suspend fun detectTag(timeoutMs: Long): NFCTagDetectionResult? {
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
            } catch (e: NFCException) {
                Log.e(TAG, "❌ Error retrieving card UUID ${e.error}")
                deferred.complete(null)
            } catch (e: AcquirerNFCException) {
                Log.e(TAG, "❌ Error retrieving card UUID ${e.event}")
                deferred.complete(null)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error retrieving card UUID", e)
                abortDetection()
                deferred.complete(null)
            }

            deferred.await()
        }
    }

    override fun abortDetection() {
        try {
            stopAntenna()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Warning: Failed to cancel detection", e)
        }
    }

    private fun doDetect(timeoutMs: Long): ByteArray {
        return try {
            if (!startAntenna()) throw NFCException(
                ProcessingErrorEvent.NFC_TAG_REACH_TIMEOUT
            )

            val cardSerial = detectDirectly(timeoutMs) ?: throw NFCException(
                ProcessingErrorEvent.NFC_TAG_REACH_TIMEOUT
            )
            abortDetection()

            cardSerial
        } catch (e: PlugPagException) {
            plugpag.stopNFCCardDirectly()
            throw e
        }
    }

    private fun detectDirectly(timeoutMs: Long): ByteArray? {
        return try {
            val timeoutInt = timeoutMs.toInt() / 1000
            val detect = plugpag.detectNfcCardDirectly(
                PlugPagNearFieldCardData.ONLY_M,
                timeoutInt
            )

            if (detect.result == PlugPag.NFC_RET_OK && detect.serialNumber != null) {
                detect.serialNumber!!
            } else {
                abortDetection()
                null
            }
        } catch (e: Exception) {
            abortDetection()
            null
        }
    }

    // ==================== Antenna Control ====================

    override fun startAntenna(): Boolean {
        return try {
            val start = plugpag.startNFCCardDirectly()
            if (start != PlugPag.NFC_RET_OK) {
                stopAntenna()
                false
            } else {
                true
            }
        } catch (e: PlugPagException) {
            stopAntenna()
            false
        }
    }

    override fun stopAntenna(): Boolean {
        return try {
            val stop = plugpag.stopNFCCardDirectly()
            stop == PlugPag.NFC_RET_OK
        } catch (e: PlugPagException) {
            false
        }
    }

    // ==================== Authentication ====================

    private suspend fun authenticateWithKey(
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
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to authenticate", e)
                deferred.complete(false)
            }

            deferred.await()
        } ?: false
    }

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

            if (!startAntenna()) return false
            val result = plugpag.authNFCCardDirectly(cardData, timeoutInt)
            return result == PlugPag.NFC_RET_OK
        } catch (e: PlugPagException) {
            throw e
        } catch (e: Exception) {
            throw e
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
            val absoluteBlockNum = (sectorNum * 4) + blockNum
            val isKeyTypeA = keyType == NFCTagSectorKeyType.A
            val pagseguroKeyType = if (isKeyTypeA) EM1KeyType.TYPE_A.ordinal else EM1KeyType.TYPE_B.ordinal

            val didAuth = authenticateWithKey(sectorNum, keyHex, keyType, 1000L)
            if (!didAuth) return null

            val cardData = PlugPagSimpleNFCData(
                pagseguroKeyType,
                absoluteBlockNum,
                ByteArray(16)
            )

            val read = plugpag.readNFCCardDirectly(cardData)
            val isSuccess = read.result == PlugPag.NFC_RET_OK
            val data = read.slots[read.startSlot]["data"]
            val dataIsValid = data != null

            if (isSuccess && dataIsValid) data else null
        } catch (e: PlugPagException) {
            null
        } catch (e: Exception) {
            null
        } finally {
            stopAntenna()
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
            val absoluteBlockNum = (sectorNum * 4) + blockNum
            val isKeyTypeA = keyType == NFCTagSectorKeyType.A
            val pagseguroKeyType = if (isKeyTypeA) EM1KeyType.TYPE_A.ordinal else EM1KeyType.TYPE_B.ordinal

            val didAuth = authenticateWithKey(sectorNum, keyHex, keyType, 1000L)
            if (!didAuth) return false

            val cardData = PlugPagSimpleNFCData(
                pagseguroKeyType,
                absoluteBlockNum,
                data
            )

            val result = plugpag.writeToNFCCardDirectly(cardData)
            result == PlugPag.NFC_RET_OK
        } catch (e: PlugPagException) {
            false
        } catch (e: Exception) {
            false
        } finally {
            stopAntenna()
        }
    }
}
