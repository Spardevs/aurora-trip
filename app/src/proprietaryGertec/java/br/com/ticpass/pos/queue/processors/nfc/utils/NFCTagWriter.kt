package br.com.ticpass.pos.queue.processors.nfc.utils

import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys

/**
 * NFC Tag Writer utility (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
object NFCTagWriter {
    private const val TAG = "NFCTagWriter"
    
    /**
     * Writes data to a block (NO-OP)
     * 
     * @return false - always fails in proprietary variant
     */
    suspend fun writeBlock(
        sector: Int,
        block: Int,
        data: ByteArray,
        keys: NFCTagSectorKeys
    ): Boolean {
        return false
    }
    
    /**
     * Writes data to multiple blocks in a sector (NO-OP)
     * 
     * @return false - always fails in proprietary variant
     */
    suspend fun writeSector(
        sector: Int,
        blocks: Map<Int, ByteArray>,
        keys: NFCTagSectorKeys
    ): Boolean {
        return false
    }
}
