package br.com.ticpass.pos.core.queue.processors.nfc.utils

import br.com.ticpass.pos.core.nfc.models.NFCTagDetectionResult
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys

/**
 * Interface for NFC operations abstraction.
 * Each flavor provides its own implementation using its SDK.
 * 
 * This allows the shared code in main/ to use NFC operations
 * without depending on flavor-specific SDK classes.
 */
interface NFCOperations {
    
    /**
     * Detects an NFC tag and retrieves its UUID.
     * 
     * @param timeoutMs Timeout for card detection
     * @return NFCTagDetectionResult with UUID, or null if detection failed
     */
    suspend fun detectTag(timeoutMs: Long = 10000L): NFCTagDetectionResult?
    
    /**
     * Aborts ongoing tag detection.
     */
    fun abortDetection()
    
    /**
     * Reads a block from the specified sector.
     * Automatically tries available keys (A first, then B).
     * 
     * @param sectorNum The sector number to read from
     * @param blockNum The relative block number within the sector (0-3 for most sectors)
     * @param sectorKeys Available keys for the sector
     * @return ByteArray containing the block data, or null if read failed
     */
    suspend fun readBlock(
        sectorNum: Int,
        blockNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): ByteArray?
    
    /**
     * Reads the sector trailer (last block) of a sector.
     * 
     * @param sectorNum The sector number
     * @param sectorKeys Available keys for the sector
     * @return ByteArray containing the sector trailer data, or null if read failed
     */
    suspend fun readSectorTrailer(
        sectorNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): ByteArray?
    
    /**
     * Writes a block to the specified sector.
     * Automatically tries available keys (A first, then B).
     * 
     * @param sectorNum The sector number to write to
     * @param blockNum The relative block number within the sector (0-3 for most sectors)
     * @param data The data to write (must be 16 bytes)
     * @param sectorKeys Available keys for the sector
     * @return true if write succeeded, false otherwise
     */
    suspend fun writeBlock(
        sectorNum: Int,
        blockNum: Int,
        data: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean
    
    /**
     * Writes the sector trailer (last block) of a sector.
     * 
     * @param sectorNum The sector number
     * @param newTrailerData The new trailer data (must be 16 bytes)
     * @param sectorKeys Available keys for the sector
     * @return true if write succeeded, false otherwise
     */
    suspend fun writeSectorTrailer(
        sectorNum: Int,
        newTrailerData: ByteArray,
        sectorKeys: NFCTagSectorKeys
    ): Boolean
    
    /**
     * Clears a data block (writes zeros).
     * Will refuse to clear sector trailer blocks.
     * 
     * @param sectorNum The sector number
     * @param blockNum The relative block number (0-2 for most sectors)
     * @param sectorKeys Available keys for the sector
     * @return true if clear succeeded, false otherwise
     */
    suspend fun clearDataBlock(
        sectorNum: Int,
        blockNum: Int,
        sectorKeys: NFCTagSectorKeys
    ): Boolean
    
    /**
     * Starts the NFC antenna.
     * @return true if started successfully
     */
    fun startAntenna(): Boolean
    
    /**
     * Stops the NFC antenna.
     * @return true if stopped successfully
     */
    fun stopAntenna(): Boolean
}
