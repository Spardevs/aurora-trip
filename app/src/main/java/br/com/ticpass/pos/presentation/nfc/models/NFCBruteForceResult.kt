package br.com.ticpass.pos.presentation.nfc.models

/**
 * Result of brute force operation
 */
data class NFCBruteForceResult(
    val totalAttempts: Int,
    val foundKeys: Map<Int, NFCTagSectorKeys>,
    val phase1Attempts: Int,
    val phase2Attempts: Int,
    val wasAborted: Boolean,
    val completeSectors: Int,
    val partialSectors: Int,
    val emptySectors: Int
)
