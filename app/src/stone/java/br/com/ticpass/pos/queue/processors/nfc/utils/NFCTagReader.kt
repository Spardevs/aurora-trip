package br.com.ticpass.pos.core.queue.processors.nfc.utils

import android.util.Log
import br.com.stone.posandroid.hal.api.mifare.MifareKeyType
import br.com.stone.posandroid.providers.PosMifareProvider
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCUtils.byteArrayToHexString

/**
 * Utility for reading NFC tag blocks with automatic key fallback
 * Always tries Key A first, then Key B if Key A fails
 */
object NFCTagReader {
    private const val TAG = "NFCTagReader"

    /**
     * Read a block from the specified sector with automatic key fallback
     * @param sectorNum The sector number to read from
     * @param blockNum The relative block number within the sector (0-3 for most sectors)
     * @param sectorKeys Available keys for the sector
     * @return ByteArray containing the block data, or null if read failed with both keys
     */
    suspend fun readBlock(
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

        // Try Key B if Key A failed or wasn't available
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

    /**
     * Read the sector trailer (last block) of a sector
     * @param sectorNum The sector number
     * @param sectorKeys Available keys for the sector
     * @return ByteArray containing the sector trailer data, or null if read failed
     */
    suspend fun readSectorTrailer(
        sectorNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): ByteArray? {
        val trailerBlockNum = if (sectorNum < 32) 3 else 15
        return readBlock(sectorNum, trailerBlockNum, sectorKeys)
    }

    /**
     * Attempt to read a block with a specific key
     */
    private suspend fun tryReadWithKey(
        sectorNum: Int,
        blockNum: Int,
        keyHex: String,
        keyType: NFCTagSectorKeyType
    ): ByteArray? {
        return try {
            val provider = NFCAuthenticator.authenticateWithKey(
                sectorNum,
                keyHex,
                keyType,
                1000L
            )

            if (provider == null) {
                Log.d(TAG, "⚠️ Authentication failed with Key $keyType")
                return null
            }

            try {
                val blockData = ByteArray(16)
                provider.readBlock(sectorNum.toByte(), blockNum.toByte(), blockData)
                Log.d(TAG, "📖 Read block data: ${byteArrayToHexString(blockData)}")
                blockData
            }
            catch (e: PosMifareProvider.MifareException) {
                Log.d(TAG, "⚠️ Read failed with Key $keyType: ${e.errorEnum}, $sectorNum, $blockNum, $keyType, $keyHex")
                null
            }
            finally {
                provider.powerOff()
            }

        }
        catch (e: PosMifareProvider.MifareException) {
            Log.d(TAG, "⚠️ Read failed with Key $keyType: ${e.errorEnum}, $sectorNum, $blockNum, $keyType, $keyHex")
            null
        }
        catch (e: Exception) {
            Log.d(TAG, "⚠️ Read failed with Key $keyType: ${e.message}")
            null
        }
    }
}
