package br.com.ticpass.pos.queue.processors.nfc.utils

import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys

/**
 * NFC Tag Formatter utility (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
object NFCTagFormatter {
    private const val TAG = "NFCTagFormatter"
    
    /**
     * Formats an NFC tag (NO-OP)
     * 
     * @return false - always fails in proprietary variant
     */
    suspend fun formatTag(
        sectorKeys: Map<Int, NFCTagSectorKeys>,
        maxSectors: Int = 16
    ): Boolean {
        return false
    }
}
