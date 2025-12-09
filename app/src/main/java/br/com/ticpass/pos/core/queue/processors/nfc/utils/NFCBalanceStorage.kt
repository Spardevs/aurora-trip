package br.com.ticpass.pos.core.queue.processors.nfc.utils

import android.util.Log
import br.com.ticpass.pos.core.nfc.models.NFCTagBalanceHeader
import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeys
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.processors.nfc.exceptions.NFCException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for reading and writing balance data to NFC tags.
 * Balance is stored in sector 0, block 2 (16 bytes).
 * 
 * Storage format:
 * - 3 bytes for balance amount (max 16,777,215 cents = ~$167,772.15)
 * - 6 bytes for timestamp
 * - Remaining bytes for magic/type/padding
 * 
 * This is acquirer-agnostic and uses the injected NFCOperations interface.
 */
@Singleton
class NFCBalanceStorage @Inject constructor(
    private val nfcOperations: NFCOperations
) {
    companion object {
        private const val TAG = "NFCBalanceStorage"
        private const val BALANCE_SECTOR = 0
        private const val BALANCE_BLOCK = 2  // Block 2 in sector 0
    }
    
    /**
     * Reads the balance from sector 0, block 2
     * @param sectorKeys Keys for authentication
     * @return NFCTagBalanceHeader or null if not found/invalid
     */
    suspend fun readBalance(sectorKeys: NFCTagSectorKeys): NFCTagBalanceHeader? {
        return try {
            val balanceBlock = nfcOperations.readBlock(BALANCE_SECTOR, BALANCE_BLOCK, sectorKeys)
            if (balanceBlock == null) {
                Log.w(TAG, "❌ Failed to read balance from sector $BALANCE_SECTOR, block $BALANCE_BLOCK")
                return null
            }
            
            val header = NFCTagBalanceHeader.fromByteArray(balanceBlock)
            if (header != null) {
                Log.d(TAG, "✅ Balance read: ${header.formattedBalance()} (${header.balance} units)")
            } else {
                Log.d(TAG, "ℹ️ No balance data found on tag")
            }
            
            header
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading balance: ${e.message}", e)
            null
        }
    }
    
    /**
     * Writes the balance to sector 0, block 2
     * @param balance Balance amount in cents (0-16,777,215)
     * @param sectorKeys Keys for authentication
     * @return The written NFCTagBalanceHeader
     */
    suspend fun writeBalance(balance: UInt, sectorKeys: NFCTagSectorKeys): NFCTagBalanceHeader {
        try {
            // Validate balance doesn't exceed 3-byte max
            if (balance > NFCTagBalanceHeader.MAX_BALANCE) {
                Log.e(TAG, "❌ Balance $balance exceeds maximum ${NFCTagBalanceHeader.MAX_BALANCE}")
                throw NFCException(ProcessingErrorEvent.NFC_BALANCE_INVALID_AMOUNT)
            }
            
            val header = NFCTagBalanceHeader(
                balance = balance,
                timestamp = System.currentTimeMillis()
            )
            
            val headerBytes = header.toByteArray()
            val success = nfcOperations.writeBlock(BALANCE_SECTOR, BALANCE_BLOCK, headerBytes, sectorKeys)
            
            if (!success) {
                Log.e(TAG, "❌ Failed to write balance")
                throw NFCException(ProcessingErrorEvent.NFC_BALANCE_WRITE_ERROR)
            }
            
            Log.i(TAG, "✅ Balance written: ${header.formattedBalance()} (${header.balance} units)")
            return header
            
        } catch (e: NFCException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error writing balance: ${e.message}", e)
            throw NFCException(ProcessingErrorEvent.NFC_BALANCE_WRITE_ERROR)
        }
    }
    
    /**
     * Clears the balance (sets to 0)
     * @param sectorKeys Keys for authentication
     * @return The written NFCTagBalanceHeader with balance = 0
     */
    suspend fun clearBalance(sectorKeys: NFCTagSectorKeys): NFCTagBalanceHeader {
        Log.d(TAG, "🗑️ Clearing balance...")
        return writeBalance(0u, sectorKeys)
    }
}
