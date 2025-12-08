package br.com.ticpass.pos.core.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.core.nfc.models.NFCTagModel
import br.com.ticpass.pos.core.nfc.models.NFCTagManufacturer
import br.com.ticpass.pos.core.nfc.models.NFCTagData
import br.com.ticpass.pos.core.nfc.models.NFCTagDataHeader
import br.com.ticpass.pos.core.nfc.models.NFCTagDataType
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorData
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCUtils.hexStringToByteArray

/**
 * Utility for mapping and reading complete NFC tag data
 * Uses NFCTagReader to read all sectors and blocks from a MIFARE Classic card
 */
object NFCTagMapper {
    private const val TAG = "NFCTagMapper"
    private const val BYTES_PER_BLOCK = 16

    /**
     * Reads complete tag data from an NFC card
     * @param sectorKeys Map of sector numbers to their keys
     * @param maxSectors Maximum number of sectors to read (default 16 for MIFARE Classic 1K)
     * @return NFCTagData object with complete card information, or null if reading failed
     */
    suspend fun readTagData(
        sectorKeys: Map<Int, NFCTagSectorKeys>,
        maxSectors: Int = 16
    ): NFCTagData? {
        Log.d(TAG, "📖 Reading complete tag data for $maxSectors sectors")

        val sectors = mutableListOf<NFCTagSectorData>()
        var uid: ByteArray? = null
        var bcc: ByteArray? = null
        var totalCardMemory = 0
        var usedCardMemory = 0

        try {
            // Read all sectors
            for (sectorNum in 0 until maxSectors) {
                val keys = sectorKeys[sectorNum]
                if (keys == null) {
                    Log.w(TAG, "⚠️ No keys available for sector $sectorNum - skipping")
                    continue
                }

                Log.d(TAG, "📖 Reading sector $sectorNum")
                val sectorData = readSectorData(sectorNum, keys)
                
                if (sectorData != null) {
                    sectors.add(sectorData)
                    totalCardMemory += sectorData.totalMemoryBytes
                    usedCardMemory += sectorData.usedMemoryBytes
                    
                    // Extract UID and BCC from sector 0, block 0
                    if (sectorNum == 0 && sectorData.blocks.isNotEmpty()) {
                        val manufacturerBlock = sectorData.blocks[0]
                        if (manufacturerBlock.size >= 9) {
                            // MIFARE Classic manufacturer block structure:
                            // Byte 0: Chip type (0x31 for 1K)
                            // Bytes 1-4: UID continuation (4C9803E6)
                            // Byte 5: BCC (Block Check Character) (08)
                            // Byte 6: Manufacturer code (04 for NXP)
                            // Byte 7: Reserved/padding (00)
                            // Bytes 8-15: Additional manufacturer data
                            
                            // Full UID = first byte + UID continuation (bytes 1-4)
                            uid = byteArrayOf(manufacturerBlock[0]) + manufacturerBlock.sliceArray(1..4)
                            bcc = manufacturerBlock.sliceArray(5..5) // BCC at byte 5
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ Failed to read sector $sectorNum")
                }
            }

            if (sectors.isEmpty()) {
                Log.w(TAG, "❌ No sectors could be read")
                return null
            }

            // Determine manufacturer and model based on manufacturer block
            val manufacturerBlock = sectors.firstOrNull()?.blocks?.firstOrNull()
            val manufacturer = determineManufacturer(manufacturerBlock)
            val model = determineModel(manufacturerBlock, sectors.size)

            Log.i(TAG, "✅ Successfully read ${sectors.size} sectors from $manufacturer $model")
            Log.i(TAG, "📊 Memory usage: $usedCardMemory/$totalCardMemory bytes")
            
            return NFCTagData(
                manufacturer = manufacturer,
                uid = uid ?: byteArrayOf(),
                bcc = bcc ?: byteArrayOf(),
                model = model,
                sectors = sectors,
                totalMemoryBytes = totalCardMemory,
                usedMemoryBytes = usedCardMemory
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading tag data: ${e.message}", e)
            return null
        }
    }

    /**
     * Reads customer data from an NFC card based on header boundaries
     * @param sectorKeys Map of sector numbers to their keys
     * @return NFCTagData object with customer data only, or null if reading failed
     */
    suspend fun readCustomerData(
        sectorKeys: NFCTagSectorKeys,
    ): NFCTagData? {
        Log.d(TAG, "📖 Reading customer data with boundary awareness")

        try {
            val headerBlock = NFCTagReader.readBlock(0, 1, sectorKeys)
            if (headerBlock == null) {
                Log.w(TAG, "❌ Failed to read header block from sector 0, block 1")
                return null
            }

            val header = NFCTagDataHeader.fromByteArray(headerBlock)
            if (header == null) {
                Log.w(TAG, "❌ Invalid or missing data header")
                return null
            }

            if (header.dataType != NFCTagDataType.CUSTOMER) {
                Log.w(TAG, "❌ Header indicates non-customer data type: ${header.dataType}")
                return null
            }

            Log.i(TAG, "📋 Found customer data header: ends at sector ${header.endSector}, block ${header.endBlock}, ${header.totalBytes} bytes")

            val sectors = mutableListOf<NFCTagSectorData>()
            var uid: ByteArray? = null
            var bcc: ByteArray? = null
            var totalCardMemory = 0
            var usedCardMemory = 0

            // Read sectors up to the boundary specified in header
            for (sectorNum in 0..header.endSector) {
                Log.d(TAG, "📖 Reading sector $sectorNum")
                val sectorData = if (sectorNum == header.endSector) {
                    // For the last sector, clear orphan blocks beyond the boundary
                    readSectorDataWithBoundary(sectorNum, sectorKeys, header.endBlock)
                } else {
                    readSectorData(sectorNum, sectorKeys)
                }

                if (sectorData != null) {
                    sectors.add(sectorData)
                    totalCardMemory += sectorData.totalMemoryBytes
                    usedCardMemory += sectorData.usedMemoryBytes

                    // Extract UID and BCC from sector 0, block 0
                    if (sectorNum == 0 && sectorData.blocks.isNotEmpty()) {
                        val manufacturerBlock = sectorData.blocks[0]
                        if (manufacturerBlock.size >= 9) {
                            uid = byteArrayOf(manufacturerBlock[0]) + manufacturerBlock.sliceArray(1..4)
                            bcc = manufacturerBlock.sliceArray(5..5)
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ Failed to read sector $sectorNum")
                }
            }

            if (sectors.isEmpty()) {
                Log.w(TAG, "❌ No sectors could be read")
                return null
            }

            // Determine manufacturer and model based on manufacturer block
            val manufacturerBlock = sectors.firstOrNull()?.blocks?.firstOrNull()
            val manufacturer = determineManufacturer(manufacturerBlock)
            val model = determineModel(manufacturerBlock, sectors.size)

            Log.i(TAG, "✅ Successfully read customer data from ${sectors.size} sectors")
            Log.i(TAG, "📊 Customer data: ${header.totalBytes} bytes across sectors 0-${header.endSector}")

            return NFCTagData(
                manufacturer = manufacturer,
                uid = uid ?: byteArrayOf(),
                bcc = bcc ?: byteArrayOf(),
                model = model,
                sectors = sectors,
                totalMemoryBytes = totalCardMemory,
                usedMemoryBytes = usedCardMemory
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading customer data: ${e.message}", e)
            return null
        }
    }

    /**
     * Reads all blocks from a single sector and calculates memory usage
     */
    private suspend fun readSectorData(
        sectorNum: Int,
        NFCTagSectorKeys: NFCTagSectorKeys
    ): NFCTagSectorData? {
        try {
            val blocks = mutableListOf<ByteArray>()
            val blocksPerSector = if (sectorNum < 32) 4 else 16
            
            // Calculate useful data blocks (excluding sector trailer and manufacturer block for sector 0)
            val dataBlocksCount = if (sectorNum == 0) {
                blocksPerSector - 2 // Exclude manufacturer block and sector trailer
            } else {
                blocksPerSector - 1 // Exclude sector trailer only
            }
            val totalUsefulSectorMemory = dataBlocksCount * BYTES_PER_BLOCK
            
            // Read all blocks in the sector
            for (blockNum in 0 until blocksPerSector) {
                val blockData = NFCTagReader.readBlock(sectorNum, blockNum, NFCTagSectorKeys)
                if (blockData != null) {
                    blocks.add(blockData)
                } else {
                    Log.w(TAG, "⚠️ Failed to read sector $sectorNum block $blockNum")
                    return null
                }
            }

            // Extract access bits and GPB from sector trailer (last block)
            val trailerBlock = blocks.lastOrNull()
            if (trailerBlock != null && trailerBlock.size >= 16) {
                val accessBits = trailerBlock.sliceArray(6..8)
                val gpb = trailerBlock.sliceArray(9..9) // General Purpose Byte
                
                // Use the actual authentication keys that were passed in
                val authKeyA = NFCTagSectorKeys.typeA ?: ""
                val authKeyB = NFCTagSectorKeys.typeB ?: ""

                Log.d(TAG, "🔑 Sector $sectorNum using authentication keys:")
                Log.d(TAG, "   Auth Key A: $authKeyA")
                Log.d(TAG, "   Auth Key B: $authKeyB")
                Log.d(TAG, "   Access bits: ${accessBits.joinToString("") { "%02X".format(it) }}")
                Log.d(TAG, "   GPB: ${gpb.joinToString("") { "%02X".format(it) }}")
                
                // Calculate used memory (non-zero bytes in data blocks, excluding sector trailer)
                val dataBlocks = blocks.dropLast(1) // Exclude sector trailer
                val usedSectorMemory = calculateUsedMemory(dataBlocks, sectorNum == 0)
                
                return NFCTagSectorData(
                    blocks = blocks,
                    keyA = hexStringToByteArray(authKeyA),
                    keyB = hexStringToByteArray(authKeyB),
                    accessBits = accessBits,
                    gpb = gpb,
                    totalMemoryBytes = totalUsefulSectorMemory,
                    usedMemoryBytes = usedSectorMemory
                )
            } else {
                Log.w(TAG, "⚠️ Invalid sector trailer in sector $sectorNum")
                return null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading sector $sectorNum: ${e.message}", e)
            return null
        }
    }

    /**
     * Calculates used memory by counting non-zero bytes in useful data blocks only
     */
    private fun calculateUsedMemory(dataBlocks: List<ByteArray>, isManufacturerSector: Boolean): Int {
        var usedBytes = 0
        
        dataBlocks.forEachIndexed { blockIndex, block ->
            if (isManufacturerSector && blockIndex == 0) {
                // Skip manufacturer block - it's not useful for data storage
                // Don't count it as "used" since it's system data
                return@forEachIndexed
            } else {
                // Count non-zero bytes in actual data blocks
                usedBytes += block.count { it != 0.toByte() }
            }
        }
        
        return usedBytes
    }

    /**
     * Determines the manufacturer based on manufacturer block
     */
    private fun determineManufacturer(manufacturerBlock: ByteArray?): NFCTagManufacturer {
        if (manufacturerBlock == null || manufacturerBlock.size < 7) return NFCTagManufacturer.UNKNOWN
        
        // Manufacturer code is at byte 6 in MIFARE Classic manufacturer block
        val manufacturerCode = manufacturerBlock[6].toInt() and 0xFF
        return NFCTagManufacturer.fromCode(manufacturerCode)
    }

    /**
     * Determines the card model based on manufacturer block and sector count
     */
    private fun determineModel(manufacturerBlock: ByteArray?, sectorCount: Int): NFCTagModel {
        if (manufacturerBlock == null || manufacturerBlock.isEmpty()) {
            return NFCTagModel.fromSectorCount(sectorCount)
        }
        
        // Chip type is at byte 0 in MIFARE Classic manufacturer block
        val chipType = manufacturerBlock[0].toInt() and 0xFF
        val modelFromChip = NFCTagModel.fromChipType(chipType)
        
        // If chip type is unknown, fall back to sector count
        return if (modelFromChip != NFCTagModel.UNKNOWN) modelFromChip else NFCTagModel.fromSectorCount(sectorCount)
    }

    /**
     * Reads sector data with boundary awareness, clearing orphan blocks in the last sector
     */
    private suspend fun readSectorDataWithBoundary(
        sectorNum: Int,
        sectorKeys: NFCTagSectorKeys,
        endBlock: Int
    ): NFCTagSectorData? {
        try {
            val blocks = mutableListOf<ByteArray>()
            val blocksPerSector = if (sectorNum < 32) 4 else 16
            
            // Calculate useful data blocks (excluding sector trailer)
            val dataBlocksCount = if (sectorNum == 0) {
                blocksPerSector - 2 // Exclude manufacturer block and sector trailer
            } else {
                blocksPerSector - 1 // Exclude sector trailer only
            }
            val totalUsefulSectorMemory = dataBlocksCount * BYTES_PER_BLOCK
            
            // Read all blocks in the sector
            for (blockNum in 0 until blocksPerSector) {
                val blockData = if (blockNum <= endBlock || blockNum == blocksPerSector - 1) {
                    // Read actual data for blocks within boundary and sector trailer
                    NFCTagReader.readBlock(sectorNum, blockNum, sectorKeys)
                } else {
                    // Return empty block for orphan blocks beyond boundary (don't write to tag)
                    Log.d(TAG, "🧹 Masking orphan block $blockNum in sector $sectorNum with empty data")
                    ByteArray(BYTES_PER_BLOCK) { 0x00 }
                }
                
                if (blockData != null) {
                    blocks.add(blockData)
                } else {
                    Log.w(TAG, "⚠️ Failed to read sector $sectorNum block $blockNum")
                    return null
                }
            }

            // Extract access bits and GPB from sector trailer (last block)
            val trailerBlock = blocks.lastOrNull()
            if (trailerBlock != null && trailerBlock.size >= 16) {
                val accessBits = trailerBlock.sliceArray(6..8)
                val gpb = trailerBlock.sliceArray(9..9)
                
                val authKeyA = sectorKeys.typeA ?: ""
                val authKeyB = sectorKeys.typeB ?: ""

                Log.d(TAG, "🔑 Sector $sectorNum boundary-aware read completed")
                
                // Calculate used memory (non-zero bytes in data blocks, excluding sector trailer)
                val dataBlocks = blocks.dropLast(1) // Exclude sector trailer
                val usedSectorMemory = calculateUsedMemory(dataBlocks, sectorNum == 0)
                
                return NFCTagSectorData(
                    blocks = blocks,
                    keyA = hexStringToByteArray(authKeyA),
                    keyB = hexStringToByteArray(authKeyB),
                    accessBits = accessBits,
                    gpb = gpb,
                    totalMemoryBytes = totalUsefulSectorMemory,
                    usedMemoryBytes = usedSectorMemory
                )
            } else {
                Log.w(TAG, "⚠️ Invalid sector trailer in sector $sectorNum")
                return null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading sector $sectorNum with boundary: ${e.message}", e)
            return null
        }
    }
}
