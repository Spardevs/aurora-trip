package br.com.ticpass.pos.queue.processors.nfc.utils

import br.com.ticpass.pos.nfc.models.NFCTagData
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys

/**
 * NFC Tag Mapper utility (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
object NFCTagMapper {
    private const val TAG = "NFCTagMapper"
    
    /**
     * Reads and maps tag data (NO-OP)
     * 
     * @return null - always fails in proprietary variant
     */
    suspend fun readTagData(
        sectorKeys: Map<Int, NFCTagSectorKeys>,
        maxSectors: Int = 16
    ): NFCTagData? {
        return null
    }
}
