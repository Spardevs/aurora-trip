package br.com.ticpass.pos.core.nfc.models

/**
 * Progress callback interface for brute force operations
 */
interface NFCBruteForceProgressCallback {
    /**
     * Called when a key attempt is made
     * @param currentAttempt Current attempt number (1-based)
     * @param maxPossibleAttempts Total number of attempts that will be made
     * @param currentKey The key being tested
     * @param sector The sector being tested
     * @param keyType The key type being tested (A or B)
     * @param phase Current phase (1 or 2)
     */
    fun onKeyAttempt(
        currentAttempt: Int,
        maxPossibleAttempts: Int,
        currentKey: String,
        sector: Int,
        keyType: NFCTagSectorKeyType,
        phase: Int
    )

    /**
     * Called when a key is successfully found
     * @param sector The sector where the key was found
     * @param keyType The key type that was found
     * @param key The successful key
     * @param completeSectors Number of sectors with both keys found
     * @param totalSectors Total number of sectors being tested
     */
    fun onKeyFound(
        sector: Int,
        keyType: NFCTagSectorKeyType,
        key: String,
        completeSectors: Int,
        totalSectors: Int
    )

    /**
     * Called when a phase completes
     * @param phase The phase that completed (1 or 2)
     * @param completeSectors Number of sectors with both keys found
     * @param partialSectors Number of sectors with only one key found
     * @param attempts Number of attempts made in this phase
     */
    fun onPhaseComplete(
        phase: Int,
        completeSectors: Int,
        partialSectors: Int,
        attempts: Int
    )
}
