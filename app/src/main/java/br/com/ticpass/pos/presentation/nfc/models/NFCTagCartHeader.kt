package br.com.ticpass.pos.presentation.nfc.models

/**
 * Data class to represent cart data boundary information for NFC tags
 * Stored in Sector 0, Block 2
 */
data class NFCTagCartHeader(
    val dataType: NFCTagDataType = NFCTagDataType.CART,
    val startSector: Int,
    val startBlock: Int,
    val endSector: Int,
    val endBlock: Int,
    val itemCount: Int,
    val totalBytes: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts the cart header to a 16-byte array for NFC storage
     * Format: [Magic:2][Type:1][Start:2][End:2][Count:1][Bytes:2][Timestamp:6]
     */
    fun toByteArray(): ByteArray {
        val buffer = ByteArray(16)
        buffer[0] = 0x43.toByte()  // Magic bytes "C" (Cart) - 0x43 is ASCII 'C'
        buffer[1] = 0x48.toByte()  // "H" (Header) - 0x48 is ASCII 'H'
        buffer[2] = dataType.id
        buffer[3] = startSector.toByte()
        buffer[4] = startBlock.toByte()
        buffer[5] = endSector.toByte()
        buffer[6] = endBlock.toByte()
        buffer[7] = itemCount.toByte()
        // Total bytes as 2 bytes (little endian) - max 65535
        buffer[8] = (totalBytes and 0xFF).toByte()
        buffer[9] = ((totalBytes shr 8) and 0xFF).toByte()
        // Timestamp as 6 bytes (little endian)
        for (i in 0..5) {
            buffer[10 + i] = ((timestamp shr (i * 8)) and 0xFF).toByte()
        }
        return buffer
    }

    companion object {
        /**
         * Creates an NFCTagCartHeader from a byte array
         * @param data The 16-byte array containing header data
         * @return NFCTagCartHeader or null if data is invalid
         */
        fun fromByteArray(data: ByteArray): NFCTagCartHeader? {
            if (data.size < 16) return null
            if (data[0] != 0x43.toByte() || data[1] != 0x48.toByte()) return null

            val dataType = NFCTagDataType.fromId(data[2]) ?: return null
            if (dataType != NFCTagDataType.CART) return null
            
            val startSector = data[3].toInt() and 0xFF
            val startBlock = data[4].toInt() and 0xFF
            val endSector = data[5].toInt() and 0xFF
            val endBlock = data[6].toInt() and 0xFF
            val itemCount = data[7].toInt() and 0xFF
            val totalBytes = (data[8].toInt() and 0xFF) or
                    ((data[9].toInt() and 0xFF) shl 8)

            var timestamp = 0L
            for (i in 0..5) {
                timestamp = timestamp or ((data[10 + i].toLong() and 0xFF) shl (i * 8))
            }

            return NFCTagCartHeader(
                dataType = dataType,
                startSector = startSector,
                startBlock = startBlock,
                endSector = endSector,
                endBlock = endBlock,
                itemCount = itemCount,
                totalBytes = totalBytes,
                timestamp = timestamp
            )
        }
    }
}
