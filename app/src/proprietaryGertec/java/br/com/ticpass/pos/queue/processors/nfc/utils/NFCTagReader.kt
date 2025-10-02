package br.com.ticpass.pos.queue.processors.nfc.utils

import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys

/**
 * NFC Tag Reader utility (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
object NFCTagReader {
    private const val TAG = "NFCTagReader"
    
    /**
     * Reads a block from a sector (NO-OP)
     * 
     * @return null - always fails in proprietary variant
     */
    suspend fun readBlock(
        sector: Int,
        block: Int,
        keys: NFCTagSectorKeys
    ): ByteArray? {
        return null
    }
    
    /**
     * Reads all blocks from a sector (NO-OP)
     * 
     * @return emptyList - always fails in proprietary variant
     */
    suspend fun readSector(
        sector: Int,
        keys: NFCTagSectorKeys
    ): List<ByteArray?> {
        return emptyList()
    }
}
