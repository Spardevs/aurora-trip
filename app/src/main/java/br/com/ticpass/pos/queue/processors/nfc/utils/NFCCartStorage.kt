package br.com.ticpass.pos.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.nfc.models.NFCCartItem
import br.com.ticpass.pos.nfc.models.NFCTagCartHeader
import br.com.ticpass.pos.nfc.models.NFCTagDataHeader
import br.com.ticpass.pos.nfc.models.NFCTagDataType
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.processors.nfc.exceptions.NFCException

/**
 * Utility for reading and writing cart data to NFC tags
 * Uses sequential storage (Option A) for simplicity and efficiency
 * 
 * This is acquirer-agnostic and uses abstracted NFCTagReader/Writer interfaces
 */
object NFCCartStorage {
    private const val TAG = "NFCCartStorage"
    private const val ITEM_SIZE = 3 // 3 bytes per cart item
    private const val BLOCK_SIZE = 16
    private const val BLOCKS_PER_SECTOR = 3 // Usable blocks (excluding sector trailer)
    private const val BYTES_PER_SECTOR = BLOCK_SIZE * BLOCKS_PER_SECTOR // 48 bytes
    
    /**
     * Reads the cart header from sector 0, block 2
     * @return NFCTagCartHeader or null if not found/invalid
     */
    suspend fun readCartHeader(sectorKeys: NFCTagSectorKeys): NFCTagCartHeader? {
        return try {
            val headerBlock = NFCTagReader.readBlock(0, 2, sectorKeys)
            if (headerBlock == null) {
                Log.w(TAG, "❌ Failed to read cart header from sector 0, block 2")
                return null
            }
            
            NFCTagCartHeader.fromByteArray(headerBlock)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading cart header: ${e.message}", e)
            null
        }
    }
    
    /**
     * Writes the cart header to sector 0, block 2
     */
    suspend fun writeCartHeader(header: NFCTagCartHeader, sectorKeys: NFCTagSectorKeys) {
        try {
            val headerBytes = header.toByteArray()
            val success = NFCTagWriter.writeBlock(0, 2, headerBytes, sectorKeys)
            
            if (!success) {
                Log.e(TAG, "❌ Failed to write cart header")
                throw NFCException(ProcessingErrorEvent.NFC_CART_WRITE_ERROR)
            }
            
            Log.d(TAG, "✅ Cart header written successfully")
        } catch (e: NFCException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error writing cart header: ${e.message}", e)
            throw NFCException(ProcessingErrorEvent.NFC_CART_WRITE_ERROR)
        }
    }
    
    /**
     * Reads cart items from the tag based on header boundaries
     * @param header The cart header containing boundary information
     * @param sectorKeys Keys for authentication
     * @return List of cart items
     */
    suspend fun readCart(header: NFCTagCartHeader, sectorKeys: NFCTagSectorKeys): List<NFCCartItem> {
        try {
            Log.d(TAG, "📖 Reading cart data from sectors ${header.startSector}-${header.endSector}")
            
            val cartBytes = mutableListOf<Byte>()
            var currentSector = header.startSector
            var currentBlock = header.startBlock
            
            // Read blocks until we reach the end boundary
            while (true) {
                val blockData = NFCTagReader.readBlock(currentSector, currentBlock, sectorKeys)
                if (blockData == null) {
                    Log.e(TAG, "❌ Failed to read sector $currentSector block $currentBlock")
                    throw NFCException(ProcessingErrorEvent.NFC_CART_READ_ERROR)
                }
                
                cartBytes.addAll(blockData.toList())
                
                // Stop if we reached the end
                if (currentSector == header.endSector && currentBlock == header.endBlock) {
                    break
                }
                
                // Move to next block
                currentBlock++
                if (currentBlock >= BLOCKS_PER_SECTOR) {
                    currentBlock = 0
                    currentSector++
                }
            }
            
            // Parse items from bytes (only parse totalBytes, not all read bytes)
            val items = mutableListOf<NFCCartItem>()
            var offset = 0
            
            while (offset + ITEM_SIZE <= header.totalBytes) {
                val item = NFCCartItem.fromByteArray(cartBytes.toByteArray(), offset)
                if (item != null) {
                    items.add(item)
                }
                offset += ITEM_SIZE
            }
            
            Log.i(TAG, "✅ Successfully read ${items.size} cart items")
            return items
            
        } catch (e: NFCException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading cart: ${e.message}", e)
            throw NFCException(ProcessingErrorEvent.NFC_CART_READ_ERROR)
        }
    }
    
    /**
     * Writes cart items to the tag starting from specified position
     * @param items List of cart items to write
     * @param startSector Starting sector for cart data
     * @param startBlock Starting block within the sector
     * @param sectorKeys Keys for authentication
     * @return Updated NFCTagCartHeader with new boundaries
     */
    suspend fun writeCart(
        items: List<NFCCartItem>,
        startSector: Int,
        startBlock: Int,
        sectorKeys: NFCTagSectorKeys
    ): NFCTagCartHeader {
        try {
            Log.d(TAG, "💾 Writing ${items.size} cart items starting at sector $startSector, block $startBlock")
            
            // Convert items to bytes
            val cartBytes = items.flatMap { it.toByteArray().toList() }.toByteArray()
            val totalBytes = cartBytes.size
            
            Log.d(TAG, "📊 Cart data size: $totalBytes bytes")
            
            var currentSector = startSector
            var currentBlock = startBlock
            var dataOffset = 0
            var lastSector = startSector
            var lastBlock = startBlock
            
            // Write data blocks
            while (dataOffset < totalBytes) {
                val blockData = ByteArray(BLOCK_SIZE)
                val bytesToCopy = minOf(BLOCK_SIZE, totalBytes - dataOffset)
                
                // Copy data to block
                cartBytes.copyInto(
                    blockData,
                    0,
                    dataOffset,
                    dataOffset + bytesToCopy
                )
                
                // Write block
                val success = NFCTagWriter.writeBlock(currentSector, currentBlock, blockData, sectorKeys)
                if (!success) {
                    Log.e(TAG, "❌ Failed to write cart data to sector $currentSector block $currentBlock")
                    throw NFCException(ProcessingErrorEvent.NFC_CART_WRITE_ERROR)
                }
                
                Log.d(TAG, "✅ Written block $currentBlock in sector $currentSector ($bytesToCopy bytes)")
                
                lastSector = currentSector
                lastBlock = currentBlock
                dataOffset += BLOCK_SIZE
                
                // Move to next block
                currentBlock++
                if (currentBlock >= BLOCKS_PER_SECTOR) {
                    currentBlock = 0
                    currentSector++
                }
            }
            
            // Create and return cart header
            val header = NFCTagCartHeader(
                dataType = NFCTagDataType.CART,
                startSector = startSector,
                startBlock = startBlock,
                endSector = lastSector,
                endBlock = lastBlock,
                itemCount = items.size,
                totalBytes = totalBytes,
                timestamp = System.currentTimeMillis()
            )
            
            Log.i(TAG, "✅ Cart data written successfully: ${items.size} items, $totalBytes bytes")
            return header
            
        } catch (e: NFCException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error writing cart: ${e.message}", e)
            throw NFCException(ProcessingErrorEvent.NFC_CART_WRITE_ERROR)
        }
    }
    
    /**
     * Calculates available space for cart data based on customer data boundaries
     * @param customerHeader The customer data header
     * @return Available bytes for cart storage
     */
    fun calculateAvailableSpace(customerHeader: NFCTagDataHeader): Int {
        // Calculate where cart can start
        val cartStartSector = if (customerHeader.endBlock == 2) {
            customerHeader.endSector + 1
        } else {
            customerHeader.endSector
        }
        val cartStartBlock = if (customerHeader.endBlock == 2) {
            0
        } else {
            customerHeader.endBlock + 1
        }
        
        val maxSector = 15 // MIFARE 1K has 16 sectors (0-15)
        
        // Calculate remaining blocks
        var remainingBlocks = 0
        
        // Blocks remaining in start sector
        remainingBlocks += (BLOCKS_PER_SECTOR - cartStartBlock)
        
        // Full sectors after start sector
        val fullSectorsRemaining = maxSector - cartStartSector
        remainingBlocks += fullSectorsRemaining * BLOCKS_PER_SECTOR
        
        return remainingBlocks * BLOCK_SIZE
    }
    
    /**
     * Reads the customer data header from sector 0, block 1
     * @return NFCTagDataHeader or null if not found/invalid
     */
    suspend fun readCustomerHeader(sectorKeys: NFCTagSectorKeys): NFCTagDataHeader? {
        return try {
            val headerBlock = NFCTagReader.readBlock(0, 1, sectorKeys)
            if (headerBlock == null) {
                Log.w(TAG, "❌ Failed to read customer header from sector 0, block 1")
                return null
            }
            
            NFCTagDataHeader.fromByteArray(headerBlock)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading customer header: ${e.message}", e)
            null
        }
    }
}
