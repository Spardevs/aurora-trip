package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.queue.processors.nfc.AcquirerNFCException
import br.com.ticpass.pos.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Service responsible for NFC card formatting operations
 * Uses simple key fallback approach: try Key A first, then Key B if it fails
 */
class NFCTagFormatter {
    private val tag: String = "NFCTagFormatter"

    /**
     * Performs comprehensive card formatting by:
     * - Clearing data blocks using NFCTagWriter (try Key A, then Key B)
     * - Updating sector trailers using NFCTagWriter (try Key A, then Key B)
     */
    suspend fun performCardFormat(
        currentKeys: Map<Int, NFCTagSectorKeys>,
        finalKeys: Map<NFCTagSectorKeyType, String>,
    ) {
        var formattedSectors = 0
        var clearedBlocks = 0
        val errors = mutableListOf<String>()
        
        try {
            // Process each sector that has found keys
            for ((sectorNum, sectorKeys) in currentKeys) {
                try {
                    Log.i(tag, "🔧 Formatting sector $sectorNum...")
                    
                    val formatResult = formatSector(
                        sectorNum = sectorNum,
                        sectorKeys = sectorKeys,
                        finalKeys = finalKeys,
                        errors = errors
                    )
                    
                    formattedSectors += formatResult.formattedSectors
                    clearedBlocks += formatResult.clearedBlocks
                    
                } catch (e: NFCException) {
                    Log.e(tag, "❌ NFCException during sector $sectorNum formatting - stopping operation", e)
                    throw e
                } catch (e: AcquirerNFCException) {
                    Log.e(tag, "❌ AcquirerNFCException during sector $sectorNum formatting - stopping operation", e)
                    throw e
                } catch (e: Exception) {
                    val errorMsg = "Failed to format sector $sectorNum: ${e.message}"
                    Log.e(tag, errorMsg)
                    errors.add(errorMsg)
                }
            }
            
            Log.i(tag, "🎉 Format complete: $formattedSectors sectors formatted, $clearedBlocks blocks cleared")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Card format failed: ${e.message}")
            throw NFCException(ProcessingErrorEvent.NFC_OPERATION_ABORTED)
        }
    }
    
    /**
     * Format a single sector by clearing data blocks and updating sector trailer
     */
    private suspend fun formatSector(
        sectorNum: Int,
        sectorKeys: NFCTagSectorKeys,
        finalKeys: Map<NFCTagSectorKeyType, String>,
        errors: MutableList<String>
    ): SectorFormatResult {
        var formattedSectors = 0
        var clearedBlocks = 0
        
        Log.i(tag, "🔧 Formatting sector $sectorNum - using simple key fallback (A then B)")
        
        // STEP 1: Clear data blocks (avoiding sector trailer)
        clearedBlocks = clearDataBlocks(sectorNum, sectorKeys, errors)
        
        // STEP 2: Update sector trailer with new keys
        if (updateSectorTrailer(sectorNum, sectorKeys, finalKeys, errors)) {
            formattedSectors++
        }
        
        return SectorFormatResult(formattedSectors, clearedBlocks)
    }

    /**
     * Clear data blocks in a sector using simple key fallback
     */
    private suspend fun clearDataBlocks(
        sectorNum: Int,
        sectorKeys: NFCTagSectorKeys,
        errors: MutableList<String>
    ): Int {
        var clearedBlocks = 0
        val isManufacturerSector = (sectorNum == 0)
        
        Log.d(tag, "🧹 Clearing data blocks in sector $sectorNum")
        
        // Determine which blocks to clear (avoid sector trailer and manufacturer block 0)
        val (startBlock, endBlock, _) = NFCUtils.getSectorBlockRange(sectorNum)
        val blocksToProcess = if (isManufacturerSector) {
            // Skip block 0 (manufacturer data) in sector 0
            1 until (endBlock - startBlock)
        } else {
            // Clear all data blocks (avoid sector trailer)
            0 until (endBlock - startBlock)
        }
        
        for (relativeBlockNum in blocksToProcess) {
            if (NFCTagWriter.clearDataBlock(sectorNum, relativeBlockNum, sectorKeys)) {
                clearedBlocks++
                Log.d(tag, "✅ Cleared sector $sectorNum block $relativeBlockNum")
            } else {
                val error = "Failed to clear sector $sectorNum block $relativeBlockNum"
                Log.w(tag, "❌ $error")
                errors.add(error)
            }
        }
        
        Log.i(tag, "🧹 Cleared $clearedBlocks blocks in sector $sectorNum")
        return clearedBlocks
    }

    /**
     * Update sector trailer with new keys using simple key fallback
     */
    private suspend fun updateSectorTrailer(
        sectorNum: Int,
        sectorKeys: NFCTagSectorKeys,
        finalKeys: Map<NFCTagSectorKeyType, String>,
        errors: MutableList<String>
    ): Boolean {
        Log.d(tag, "🔑 Updating sector $sectorNum trailer with FFF keys")

        val finalKeyA = NFCUtils.hexStringToByteArray(
            finalKeys[NFCTagSectorKeyType.A] ?: "FFFFFFFFFFFF"
        )
        val finalKeyB = NFCUtils.hexStringToByteArray(
            finalKeys[NFCTagSectorKeyType.B] ?: "FFFFFFFFFFFF"
        )

        val sectorKeysBytes = mapOf(
            NFCTagSectorKeyType.A to finalKeyA,
            NFCTagSectorKeyType.B to finalKeyB
        )

        val newTrailer = NFCUtils.createProductionSectorTrailer(sectorKeysBytes)
        
        if (NFCTagWriter.writeSectorTrailer(sectorNum, newTrailer, sectorKeys)) {
            Log.i(tag, "✅ Updated sector $sectorNum keys to FFF pattern")
            return true
        } else {
            val error = "Failed to update sector $sectorNum trailer"
            Log.w(tag, "❌ $error")
            errors.add(error)
            return false
        }
    }
    
    /**
     * Data class to hold sector formatting results
     */
    private data class SectorFormatResult(
        val formattedSectors: Int,
        val clearedBlocks: Int
    )
}