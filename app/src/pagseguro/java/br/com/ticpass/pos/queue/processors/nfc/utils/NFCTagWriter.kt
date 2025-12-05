package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.EM1KeyType
import br.com.uol.pagseguro.plugpagservice.wrapper.data.request.PlugPagSimpleNFCData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException

/**
 * Utility for write NFC tag blocks with automatic key fallback
 * Always tries Key A first, then Key B if Key A fails
 */
object NFCTagWriter {
    private const val TAG = "NFCTagWriter"
    private val plugpag = AcquirerSdk.nfc.getInstance()
    private val antenna = NFCTagReaderAntenna

    /**
     * Write a block from the specified sector with automatic key fallback
     * @param sectorNum The sector number to write from
     * @param blockNum The relative block number within the sector (0-3 for most sectors)
     * @param sectorKeys Available keys for the sector
     * @return ByteArray containing the block data, or null if write failed with both keys
     */
    suspend fun writeBlock(
        sectorNum: Int,
        blockNum: Int,
        data: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        Log.d(TAG, "📖 Writing sector $sectorNum block $blockNum")

        // Try Key A first
        sectorKeys.typeA?.let { keyA ->
            Log.d(TAG, "🔑 Trying Key A: $keyA")
            val isSuccess = tryWriteWithKey(sectorNum, blockNum, data, keyA, NFCTagSectorKeyType.A)
            if (isSuccess) {
                Log.d(TAG, "✅ Write successful with Key A")
                return isSuccess
            }
        }

        // Try Key B if Key A failed or wasn't available
        sectorKeys.typeB?.let { keyB ->
            Log.d(TAG, "🔑 Trying Key B: $keyB")
            val isSuccess = tryWriteWithKey(sectorNum, blockNum, data, keyB, NFCTagSectorKeyType.B)
            if (isSuccess) {
                Log.d(TAG, "✅ Write successful with Key B")
                return isSuccess
            }
        }

        Log.w(TAG, "❌ Failed to write sector $sectorNum block $blockNum with both keys")
        return false
    }

    /**
     * Write the sector trailer (last block) of a sector
     * @param sectorNum The sector number
     * @param sectorKeys Available keys for the sector
     * @return ByteArray containing the sector trailer data, or null if write failed
     */
    suspend fun writeSectorTrailer(
        sectorNum: Int,
        newTrailerData: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        val trailerBlockNum = if (sectorNum < 32) 3 else 15
        return writeBlock(sectorNum, trailerBlockNum, newTrailerData, sectorKeys)
    }

    /**
     * Clear a data block (write zeros, avoiding sector trailer)
     * @param sectorNum The sector number
     * @param blockNum The relative block number (0-2 for most sectors, avoiding trailer)
     * @param sectorKeys Available keys for the sector
     * @return true if clear succeeded, false if both keys failed
     */
    suspend fun clearDataBlock(
        sectorNum: Int,
        blockNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        // Don't clear sector trailer blocks
        val maxDataBlock = if (sectorNum < 32) 2 else 14
        if (blockNum > maxDataBlock) {
            Log.w(TAG, "⚠️ Refusing to clear sector trailer block $blockNum in sector $sectorNum")
            return false
        }

        val emptyData = NFCUtils.createEmptyBlockData()
        Log.d(TAG, "🧹 Clearing sector $sectorNum block $blockNum")
        return writeBlock(sectorNum, blockNum, emptyData, sectorKeys)
    }

    /**
     * Attempt to write a block with a specific key
     */
    private suspend fun tryWriteWithKey(
        sectorNum: Int,
        blockNum: Int,
        data: ByteArray,
        keyHex: String,
        keyType: NFCTagSectorKeyType
    ): Boolean {
        return try {
            // calculate absolute block number
            val absoluteBlockNum = (sectorNum * 4) + blockNum
            val isKeyTypeA = keyType == NFCTagSectorKeyType.A
            val pagseguroKeyType = if (isKeyTypeA) EM1KeyType.TYPE_A.ordinal else EM1KeyType.TYPE_B.ordinal

            val didAuth = NFCAuthenticator.authenticateWithKey(
                sectorNum,
                keyHex,
                keyType,
                1000L
            )
            if(!didAuth) return false

            val cardData = PlugPagSimpleNFCData(
                pagseguroKeyType,
                absoluteBlockNum,
                data
            )

            val result = plugpag.writeToNFCCardDirectly(cardData)
            val isSuccess = result == PlugPag.NFC_RET_OK

            return isSuccess
        }
        catch (e: PlugPagException) {
            false
        }
        catch (e: Exception) {
            false
        }
        finally {
            antenna.stop()
        }
    }
}
