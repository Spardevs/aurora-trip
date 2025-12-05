package br.com.ticpass.pos.nfc.models

/**
 * Data class to represent generic data boundary information for NFC tags
 */
data class NFCTagDataHeader(
    val dataType: NFCTagDataType,
    val endSector: Int,
    val endBlock: Int,
    val totalBytes: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteArray(16) // One block size
        buffer[0] = 0x44.toByte() // Magic bytes "D" (Data) - 0x44 is ASCII 'D'
        buffer[1] = 0x48.toByte() // "H" (Header) - 0x48 is ASCII 'H'
        buffer[2] = dataType.id
        buffer[3] = endSector.toByte()
        buffer[4] = endBlock.toByte()
        // Total bytes as 4 bytes (little endian)
        buffer[5] = (totalBytes and 0xFF).toByte()
        buffer[6] = ((totalBytes shr 8) and 0xFF).toByte()
        buffer[7] = ((totalBytes shr 16) and 0xFF).toByte()
        buffer[8] = ((totalBytes shr 24) and 0xFF).toByte()
        // Timestamp as 7 bytes (little endian) - reduced to fit data type
        for (i in 0..6) {
            buffer[9 + i] = ((timestamp shr (i * 8)) and 0xFF).toByte()
        }
        return buffer
    }

    companion object {
        fun fromByteArray(data: ByteArray): NFCTagDataHeader? {
            if (data.size < 16) return null
            if (data[0] != 0x44.toByte() || data[1] != 0x48.toByte()) return null

            val dataType = NFCTagDataType.fromId(data[2]) ?: return null
            val endSector = data[3].toInt() and 0xFF
            val endBlock = data[4].toInt() and 0xFF
            val totalBytes = (data[5].toInt() and 0xFF) or
                    ((data[6].toInt() and 0xFF) shl 8) or
                    ((data[7].toInt() and 0xFF) shl 16) or
                    ((data[8].toInt() and 0xFF) shl 24)

            var timestamp = 0L
            for (i in 0..6) {
                timestamp = timestamp or ((data[9 + i].toLong() and 0xFF) shl (i * 8))
            }

            return NFCTagDataHeader(dataType, endSector, endBlock, totalBytes, timestamp)
        }
    }
}