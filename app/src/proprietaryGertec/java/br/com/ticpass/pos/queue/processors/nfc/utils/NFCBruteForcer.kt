package br.com.ticpass.pos.queue.processors.nfc.utils

import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys

/**
 * NFC Brute Forcer utility (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
object NFCBruteForcer {
    private const val TAG = "NFCBruteForcer"
    
    /**
     * Attempts to brute force sector keys (NO-OP)
     * 
     * @return null - always fails in proprietary variant
     */
    suspend fun bruteForceSectorKeys(
        sector: Int,
        commonKeys: List<ByteArray>
    ): NFCTagSectorKeys? {
        return null
    }
}
