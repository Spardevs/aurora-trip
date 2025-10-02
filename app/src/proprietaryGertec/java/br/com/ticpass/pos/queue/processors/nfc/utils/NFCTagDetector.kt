package br.com.ticpass.pos.queue.processors.nfc.utils

import br.com.ticpass.pos.nfc.models.NFCTagDetectionResult

/**
 * NFC Tag Detector utility (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Minimal stub for compilation compatibility.
 */
object NFCTagDetector {
    private const val TAG = "NFCTagDetector"
    
    /**
     * Detects an NFC card and retrieves its UUID (NO-OP)
     * 
     * @param timeoutMs Timeout for card detection
     * @return null - always fails in proprietary variant
     */
    suspend fun detectTag(timeoutMs: Long = 10000L): NFCTagDetectionResult? {
        return null
    }
}
