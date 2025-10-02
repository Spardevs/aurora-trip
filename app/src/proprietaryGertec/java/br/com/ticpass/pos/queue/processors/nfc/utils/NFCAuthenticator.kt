package br.com.ticpass.pos.queue.processors.nfc.utils

import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys

/**
 * NFC Authenticator utility (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
object NFCAuthenticator {
    private const val TAG = "NFCAuthenticator"
    
    /**
     * Authenticates a sector with provided keys (NO-OP)
     * 
     * @return false - always fails in proprietary variant
     */
    suspend fun authenticateSector(
        sector: Int,
        keys: NFCTagSectorKeys
    ): Boolean {
        return false
    }
}
