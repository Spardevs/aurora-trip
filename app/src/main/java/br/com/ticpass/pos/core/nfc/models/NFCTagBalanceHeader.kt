package br.com.ticpass.pos.core.nfc.models

import br.com.ticpass.Constants

/**
 * Data class to represent balance data stored on NFC tags.
 * Balance is stored in sector 0, block 2 (shared with cart header area).
 * 
 * Storage format: 16 bytes total
 * - [0-1]: Magic bytes "BL" (0x42, 0x4C)
 * - [2]: Data type (BALANCE = 0x03)
 * - [3-6]: Balance amount (4 bytes, little-endian, max 4,294,967,295 = ~$4.29M with factor 1000)
 * - [7-12]: Timestamp (6 bytes, little-endian)
 * - [13-15]: Reserved/padding
 */
data class NFCTagBalanceHeader(
    val dataType: NFCTagDataType = NFCTagDataType.BALANCE,
    val balance: UInt,  // Balance in smallest units based on CONVERSION_FACTOR
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val SIZE_BYTES = 16
        const val MAX_BALANCE: UInt = 4_294_967_295u  // 4 bytes max value (2^32 - 1)
        
        /**
         * Creates an NFCTagBalanceHeader from a byte array
         * @param data The 16-byte array containing balance data
         * @return NFCTagBalanceHeader or null if data is invalid
         */
        fun fromByteArray(data: ByteArray): NFCTagBalanceHeader? {
            if (data.size < SIZE_BYTES) return null
            
            // Check magic bytes "BL"
            if (data[0] != 0x42.toByte() || data[1] != 0x4C.toByte()) return null
            
            val dataType = NFCTagDataType.fromId(data[2]) ?: return null
            if (dataType != NFCTagDataType.BALANCE) return null
            
            // Read balance (4 bytes, little-endian)
            val balance = ((data[3].toInt() and 0xFF) or
                    ((data[4].toInt() and 0xFF) shl 8) or
                    ((data[5].toInt() and 0xFF) shl 16) or
                    ((data[6].toInt() and 0xFF) shl 24)).toUInt()
            
            // Read timestamp (6 bytes, little-endian)
            var timestamp = 0L
            for (i in 0..5) {
                timestamp = timestamp or ((data[7 + i].toLong() and 0xFF) shl (i * 8))
            }
            
            return NFCTagBalanceHeader(
                dataType = dataType,
                balance = balance,
                timestamp = timestamp
            )
        }
    }
    
    /**
     * Converts the balance header to a 16-byte array for NFC storage
     */
    fun toByteArray(): ByteArray {
        val buffer = ByteArray(SIZE_BYTES)
        
        // Magic bytes "BL" (Balance)
        buffer[0] = 0x42.toByte()  // 'B'
        buffer[1] = 0x4C.toByte()  // 'L'
        
        // Data type
        buffer[2] = dataType.id
        
        // Balance (4 bytes, little-endian)
        buffer[3] = (balance.toInt() and 0xFF).toByte()
        buffer[4] = ((balance.toInt() shr 8) and 0xFF).toByte()
        buffer[5] = ((balance.toInt() shr 16) and 0xFF).toByte()
        buffer[6] = ((balance.toInt() shr 24) and 0xFF).toByte()
        
        // Timestamp (6 bytes, little-endian)
        for (i in 0..5) {
            buffer[7 + i] = ((timestamp shr (i * 8)) and 0xFF).toByte()
        }
        
        // Remaining bytes are padding (already 0)
        return buffer
    }
    
    /**
     * Returns balance formatted as currency string using CONVERSION_FACTOR
     */
    fun formattedBalance(): String {
        val factor = Constants.CONVERSION_FACTOR
        val whole = balance.toLong() / factor
        val fraction = balance.toLong() % factor
        val fractionDigits = factor.toString().length - 1
        return "$${whole}.${fraction.toString().padStart(fractionDigits, '0')}"
    }
}
