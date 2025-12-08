package br.com.ticpass.pos.core.queue.processors.nfc.utils

import android.util.Log
import br.com.stone.posandroid.providers.PosMifareProvider
import br.com.ticpass.pos.core.nfc.models.NFCBruteForceProgressCallback
import br.com.ticpass.pos.core.nfc.models.NFCBruteForceResult
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.nfc.utils.nfcCommonKeys
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.core.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCBruteForce
import br.com.ticpass.pos.core.sdk.AcquirerSdk

/**
 * Comprehensive standalone NFC brute force utility for MIFARE Classic cards
 */
object NFCBruteForcer {
    private const val TAG = "NFCBruteForcer"
    
    @Volatile
    private var abortBruteForce = false
    
    /**
     * Abort the ongoing brute force operation
     */
    fun abortBruteForce() {
        Log.i(TAG, "🛑 Brute force abort requested")
        abortBruteForce = true
    }
    
    /**
     * Optimized brute force key finding method for MIFARE Classic sectors
     */
    suspend fun bruteForceKeys(
        maxSectors: Int = 16,
        timeoutPerAttempt: Long = 500L,
        ownedKeys: List<String> = emptyList(),
        mode: NFCBruteForce = NFCBruteForce.MOST_LIKELY,
        progressCallback: NFCBruteForceProgressCallback? = null
    ): NFCBruteForceResult {
        abortBruteForce = false
        val foundKeys = initializeSectorKeys(maxSectors)
        val allKeysWithGroups = buildKeyList(ownedKeys, mode)
        
        logBruteForceStart(maxSectors, ownedKeys.size, mode, allKeysWithGroups.size)

        val attempts = performBruteForce(
            foundKeys = foundKeys,
            allKeysWithGroups = allKeysWithGroups,
            maxSectors = maxSectors,
            timeoutPerAttempt = timeoutPerAttempt,
            progressCallback = progressCallback
        )

        val statistics = calculateStatistics(foundKeys, maxSectors, attempts)
        logBruteForceResults(statistics, mode, foundKeys, attempts)
        
        progressCallback?.onPhaseComplete(
            phase = 1,
            completeSectors = statistics.complete,
            partialSectors = statistics.partial,
            attempts = attempts
        )

        return NFCBruteForceResult(
            foundKeys = foundKeys,
            totalAttempts = attempts,
            phase1Attempts = attempts,
            phase2Attempts = 0,
            wasAborted = abortBruteForce,
            completeSectors = statistics.complete,
            partialSectors = statistics.partial,
            emptySectors = statistics.empty
        )
    }

    private fun initializeSectorKeys(maxSectors: Int): MutableMap<Int, NFCTagSectorKeys> {
        return (0 until maxSectors).associateWith { NFCTagSectorKeys() }.toMutableMap()
    }

    private fun buildKeyList(ownedKeys: List<String>, mode: NFCBruteForce): List<Pair<String, String>> {
        val standardKeys = nfcCommonKeys["standard set"] ?: emptyList()
        val result = mutableListOf<Pair<String, String>>()
        
        // Add owned keys first
        ownedKeys.forEach { key -> result.add(key to "additional") }
        
        when (mode) {
            NFCBruteForce.NONE -> { /* Only owned keys already added */ }
            NFCBruteForce.MOST_LIKELY -> {
                standardKeys.forEach { key ->
                    if (key !in ownedKeys) result.add(key to "standard set")
                }
            }
            NFCBruteForce.FULL -> {
                standardKeys.forEach { key ->
                    if (key !in ownedKeys) result.add(key to "standard set")
                }
                nfcCommonKeys.forEach { (groupName, keys) ->
                    if (groupName != "standard set") {
                        keys.forEach { key ->
                            if (key !in ownedKeys && key !in standardKeys) {
                                result.add(key to groupName)
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    private suspend fun performBruteForce(
        foundKeys: MutableMap<Int, NFCTagSectorKeys>,
        allKeysWithGroups: List<Pair<String, String>>,
        maxSectors: Int,
        timeoutPerAttempt: Long,
        progressCallback: NFCBruteForceProgressCallback?
    ): Int {
        var attempts = 0
        val maxPossibleAttempts = allKeysWithGroups.size * maxSectors * 2

        for ((key, keySource) in allKeysWithGroups) {
            if (abortBruteForce) {
                Log.i(TAG, "🛑 Brute force aborted")
                break
            }

            for (sector in 0 until maxSectors) {
                if (abortBruteForce) break

                attempts += tryKeyForSector(
                    key = key,
                    keySource = keySource,
                    sector = sector,
                    foundKeys = foundKeys,
                    timeoutPerAttempt = timeoutPerAttempt,
                    maxPossibleAttempts = maxPossibleAttempts,
                    maxSectors = maxSectors,
                    currentAttempt = attempts,
                    progressCallback = progressCallback
                )
            }

            if (foundKeys.values.all { it.isComplete() }) {
                Log.i(TAG, "🎉 All sectors complete! Total attempts: $attempts")
                break
            }
        }
        return attempts
    }

    private suspend fun tryKeyForSector(
        key: String,
        keySource: String,
        sector: Int,
        foundKeys: MutableMap<Int, NFCTagSectorKeys>,
        timeoutPerAttempt: Long,
        maxPossibleAttempts: Int,
        maxSectors: Int,
        currentAttempt: Int,
        progressCallback: NFCBruteForceProgressCallback?
    ): Int {
        var attemptCount = 0
        val sectorKeys = foundKeys[sector]!!

        // Try key A if not found
        if (sectorKeys.typeA == null) {
            attemptCount++
            tryKey(
                key = key,
                keySource = keySource,
                keyType = NFCTagSectorKeyType.A,
                sector = sector,
                foundKeys = foundKeys,
                timeoutPerAttempt = timeoutPerAttempt,
                maxPossibleAttempts = maxPossibleAttempts,
                maxSectors = maxSectors,
                currentAttempt = currentAttempt + attemptCount,
                progressCallback = progressCallback
            )
        }

        // Try key B if not found
        if (foundKeys[sector]!!.typeB == null) {
            attemptCount++
            tryKey(
                key = key,
                keySource = keySource,
                keyType = NFCTagSectorKeyType.B,
                sector = sector,
                foundKeys = foundKeys,
                timeoutPerAttempt = timeoutPerAttempt,
                maxPossibleAttempts = maxPossibleAttempts,
                maxSectors = maxSectors,
                currentAttempt = currentAttempt + attemptCount,
                progressCallback = progressCallback
            )
        }

        return attemptCount
    }

    private suspend fun tryKey(
        key: String,
        keySource: String,
        keyType: NFCTagSectorKeyType,
        sector: Int,
        foundKeys: MutableMap<Int, NFCTagSectorKeys>,
        timeoutPerAttempt: Long,
        maxPossibleAttempts: Int,
        maxSectors: Int,
        currentAttempt: Int,
        progressCallback: NFCBruteForceProgressCallback?
    ) {
        progressCallback?.onKeyAttempt(
            currentAttempt = currentAttempt,
            maxPossibleAttempts = maxPossibleAttempts,
            currentKey = key,
            sector = sector,
            keyType = keyType,
            phase = 1
        )

        try {
            if (tryKeyOnSector(key, sector, keyType, timeoutPerAttempt)) {
                val currentKeys = foundKeys[sector]!!
                foundKeys[sector] = if (keyType == NFCTagSectorKeyType.A) {
                    currentKeys.copy(typeA = key)
                } else {
                    currentKeys.copy(typeB = key)
                }

                Log.i(TAG, "✅ Sector $sector ${keyType.name} found with key: $key ($keySource)")

                progressCallback?.onKeyFound(
                    sector = sector,
                    keyType = keyType,
                    key = key,
                    completeSectors = foundKeys.values.count { it.isComplete() },
                    totalSectors = maxSectors
                )
            }
        } catch (e: NFCException) {
            Log.e(TAG, "❌ NFCException during ${keyType.name} brute force - stopping operation", e)
            throw e
        } catch (e: AcquirerNFCException) {
            Log.e(TAG, "❌ AcquirerNFCException during ${keyType.name} brute force - stopping operation", e)
            throw e
        }
    }

    /**
     * Try a single key on a single sector with specified key type
     */
    private suspend fun tryKeyOnSector(
        key: String,
        sector: Int,
        keyType: NFCTagSectorKeyType,
        timeoutMs: Long
    ): Boolean {
        // Check if tag is still present before attempting authentication
        val tagDetectionResult = NFCTagDetector.detectTag(timeoutMs)
        if (tagDetectionResult == null) {
            Log.w(TAG, "🏷️ Tag not detected before authentication attempt - stopping brute force")
            throw NFCException(ProcessingErrorEvent.NFC_TAG_NOT_FOUND)
        }
        
        Log.d(TAG, "🏷️ Tag detected (${tagDetectionResult.cardUUIDString}) - proceeding with authentication")

        val nfcProviderFactory = AcquirerSdk.nfc.getInstance()
        var nfcProvider: PosMifareProvider? = nfcProviderFactory()

        return try {
            nfcProvider = NFCAuthenticator.authenticateWithKey(
                sectorNum = sector,
                key = key,
                keyType = keyType,
                timeoutMs = timeoutMs
            )
            val didAuth = nfcProvider != null
            Log.d(TAG, "✅ Authentication succeeded: key=$key, sector=$sector, type=$keyType")
            didAuth
        } catch (e: PosMifareProvider.MifareException) {
            Log.d(TAG, "❌ Authentication failed: key=$key, sector=$sector, type=$keyType, error=${e.errorEnum}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error during authentication: key=$key, sector=$sector, type=$keyType", e)
            false
        } finally {
            // Clean up the provider
            try {
                nfcProvider?.powerOff()
            } catch (e: Exception) {
                Log.w(TAG, "Warning: Failed to power off NFC provider", e)
            }
        }
    }

    private data class BruteForceStatistics(
        val complete: Int,
        val partial: Int,
        val empty: Int
    )

    private fun calculateStatistics(
        foundKeys: Map<Int, NFCTagSectorKeys>,
        maxSectors: Int
    ): BruteForceStatistics {
        val complete = foundKeys.values.count { it.isComplete() }
        val partial = foundKeys.values.count { it.hasAnyKey() && !it.isComplete() }
        val empty = foundKeys.values.count { !it.hasAnyKey() }
        return BruteForceStatistics(complete, partial, empty)
    }

    private fun calculateStatistics(
        foundKeys: Map<Int, NFCTagSectorKeys>,
        maxSectors: Int,
        attempts: Int
    ): BruteForceStatistics {
        return calculateStatistics(foundKeys, maxSectors)
    }

    private fun logBruteForceStart(
        maxSectors: Int,
        ownedKeysCount: Int,
        mode: NFCBruteForce,
        totalKeys: Int
    ) {
        val standardKeysCount = nfcCommonKeys["standard set"]?.size ?: 0
        val maxPossibleAttempts = totalKeys * maxSectors * 2

        Log.i(TAG, "🚀 Starting brute force for $maxSectors sectors")
        Log.i(TAG, "📊 Additional keys: $ownedKeysCount, Standard keys: $standardKeysCount")
        Log.i(TAG, "📊 Brute force mode: $mode")
        Log.i(TAG, "📈 Total unique keys: $totalKeys")
        Log.i(TAG, "📈 Max possible attempts: $maxPossibleAttempts")
        Log.i(TAG, "🔍 Testing all selected keys across all sectors")
    }

    private fun logBruteForceResults(
        statistics: BruteForceStatistics,
        mode: NFCBruteForce,
        foundKeys: Map<Int, NFCTagSectorKeys>,
        attempts: Int
    ) {
        Log.i(TAG, "🏁 Brute force complete:")
        Log.i(TAG, "   ✅ Complete sectors (both keys): ${statistics.complete}")
        Log.i(TAG, "   🔶 Partial sectors (one key): ${statistics.partial}")
        Log.i(TAG, "   ❌ Empty sectors (no keys): ${statistics.empty}")
        Log.i(TAG, "   🔢 Total attempts: $attempts")
        Log.i(TAG, "   📊 Brute force mode: $mode")
        Log.i(TAG, "   🛑 Aborted: $abortBruteForce")
        
        // Log detailed results
        foundKeys.forEach { (sector, keys) ->
            Log.i(TAG, "   Sector $sector: $keys")
        }
    }
}
