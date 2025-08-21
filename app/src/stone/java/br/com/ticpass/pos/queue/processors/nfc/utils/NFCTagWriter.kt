package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.stone.posandroid.hal.api.mifare.MifareKeyType
import br.com.stone.posandroid.providers.PosMifareProvider
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCUtils.byteArrayToHexString

/**
 * Utility for writing NFC tag blocks with automatic key fallback
 * Always tries Key A first, then Key B if Key A fails
 */
object NFCTagWriter {
    private const val TAG = "NFCTagWriter"

    /**
     * Write data to a block in the specified sector with automatic key fallback
     * @param sectorNum The sector number to write to
     * @param blockNum The relative block number within the sector (0-3 for most sectors)
     * @param data The 16-byte data to write to the block
     * @param sectorKeys Available keys for the sector
     * @return true if write succeeded, false if both keys failed
     */
    suspend fun writeBlock(
        sectorNum: Int,
        blockNum: Int,
        data: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        Log.d(TAG, "✏️ Writing sector $sectorNum block $blockNum: ${byteArrayToHexString(data)}")

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
     * Write new sector trailer (keys and access bits) to a sector
     * @param sectorNum The sector number
     * @param newTrailerData The 16-byte sector trailer data (Key A + Access Bits + Key B)
     * @param sectorKeys Available keys for the sector
     * @return true if write succeeded, false if both keys failed
     */
    suspend fun writeSectorTrailer(
        sectorNum: Int,
        newTrailerData: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        val trailerBlockNum = if (sectorNum < 32) 3 else 15
        Log.d(TAG, "🔑 Writing sector $sectorNum trailer: ${byteArrayToHexString(newTrailerData)}")
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
            val provider = NFCAuthenticator.authenticateWithKey(
                sectorNum,
                keyHex,
                keyType,
                1000L
            )

            if (provider == null) {
                Log.d(TAG, "⚠️ Authentication failed with Key $keyType, $keyHex")
                return false
            }

            try {
                provider.writeBlock(sectorNum.toByte(), blockNum.toByte(), data)
                Log.d(TAG, "✏️ Write successful with Key $keyType, $sectorNum, $blockNum, $keyType, $keyHex")
                true
            }
            catch (e: PosMifareProvider.MifareException) {
                Log.d(TAG, "⚠️ Write failed with Key $keyType: ${e.errorEnum}, $sectorNum, $blockNum, $keyType, $keyHex")
                false
            }
            finally {
                provider.powerOff()
            }

        }
        catch (e: PosMifareProvider.MifareException) {
            Log.d(TAG, "⚠️ Write failed with Key $keyType: ${e.errorEnum}, $sectorNum, $blockNum, $keyType, $keyHex")
            false
        }
        catch (e: Exception) {
            Log.d(TAG, "⚠️ Write failed with Key $keyType: ${e}")
            false
        }
    }
}
