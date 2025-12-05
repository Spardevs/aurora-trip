package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.nfc.models.NFCTagDetectionResult
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProprietaryGertec NO-OP implementation of NFCOperations.
 * NFC functionality is not supported on ProprietaryGertec devices.
 * All operations return failure/null.
 */
@Singleton
class ProprietaryGertecNFCOperations @Inject constructor() : NFCOperations {

    companion object {
        private const val TAG = "GertecNFCOperations"
    }

    override suspend fun detectTag(timeoutMs: Long): NFCTagDetectionResult? {
        Log.w(TAG, "NO-OP: NFC tag detection not supported on ProprietaryGertec")
        return null
    }

    override fun abortDetection() {
        Log.d(TAG, "NO-OP: Abort detection called")
    }

    override suspend fun readBlock(
        sectorNum: Int,
        blockNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): ByteArray? {
        Log.w(TAG, "NO-OP: NFC read not supported on ProprietaryGertec")
        return null
    }

    override suspend fun readSectorTrailer(
        sectorNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): ByteArray? {
        Log.w(TAG, "NO-OP: NFC read sector trailer not supported on ProprietaryGertec")
        return null
    }

    override suspend fun writeBlock(
        sectorNum: Int,
        blockNum: Int,
        data: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        Log.w(TAG, "NO-OP: NFC write not supported on ProprietaryGertec")
        return false
    }

    override suspend fun writeSectorTrailer(
        sectorNum: Int,
        newTrailerData: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        Log.w(TAG, "NO-OP: NFC write sector trailer not supported on ProprietaryGertec")
        return false
    }

    override suspend fun clearDataBlock(
        sectorNum: Int,
        blockNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): Boolean {
        Log.w(TAG, "NO-OP: NFC clear not supported on ProprietaryGertec")
        return false
    }

    override fun startAntenna(): Boolean {
        Log.w(TAG, "NO-OP: NFC antenna not supported on ProprietaryGertec")
        return false
    }

    override fun stopAntenna(): Boolean {
        Log.d(TAG, "NO-OP: Stop antenna called")
        return true
    }
}
