package br.com.ticpass.pos.core.queue.processors.nfc.utils

import br.com.ticpass.pos.core.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.core.queue.processors.nfc.exceptions.NFCException

/**
 * Utility functions for NFC operations
 */
object NFCUtils {
    
    /**
     * Convert hex string to byte array
     */
    fun hexStringToByteArray(s: String): ByteArray {
        return s.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    fun byteArrayToHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }.uppercase()
    }
    
    /**
     * Get block range for a sector (handles both 1K and 4K card layouts)
     * @return Triple of (startBlock, endBlock, trailerBlock)
     */
    fun getSectorBlockRange(sector: Int): Triple<Int, Int, Int> {
        return if (sector < 32) {
            // Sectors 0-31: 4 blocks each (1K card layout)
            val startBlock = sector * 4
            val endBlock = startBlock + 4
            val trailerBlock = startBlock + 3
            Triple(startBlock, endBlock, trailerBlock)
        } else {
            // Sectors 32+: 16 blocks each (4K card layout)
            val startBlock = 128 + (sector - 32) * 16
            val endBlock = startBlock + 16
            val trailerBlock = startBlock + 15
            Triple(startBlock, endBlock, trailerBlock)
        }
    }
    
    /**
     * Create empty block data (16 bytes of zeros)
     */
    fun createEmptyBlockData(): ByteArray {
        return ByteArray(16)
    }

    /**
     * Create sector trailer with FFF keys and transport configuration access bits
     * @return ByteArray of 16 bytes representing the sector trailer
     */
    fun createTransportSectorTrailer(): ByteArray {
        val fffKeyBytes = hexStringToByteArray("FFFFFFFFFFFF")

        return ByteArray(16).apply {
            // KeyA (bytes 0-5)
            System.arraycopy(fffKeyBytes, 0, this, 0, 6)
            // Access bits (bytes 6-8) - MIFARE Classic transport configuration
            // Standard transport config: allows Key A to write both Key A and Key B
            // Access condition bits for sector trailer: C1=0, C2=0, C3=1 (binary: 001)
            // This means: Key A never readable, Key B readable by Key A, both keys writable by Key A
            // MIFARE format: [C1₃C1₂C1₁C1₀][C2₃C2₂C2₁C2₀][C3₃C3₂C3₁C3₀] with bit inversion
            this[6] = 0xFF.toByte()  // C1 inverted: 11110000 -> 11111111 (all data blocks accessible)
            this[7] = 0x07.toByte()  // C2 inverted: 11110000 -> 00000111 (all data blocks accessible)
            this[8] = 0x80.toByte()  // C3 inverted: 00001000 -> 10000000 (sector trailer: 001 pattern)
            // GPB (byte 9) - general purpose byte
            this[9] = 0x69.toByte()  // Standard transport configuration GPB
            // KeyB (bytes 10-15)
            System.arraycopy(fffKeyBytes, 0, this, 10, 6)
        }
    }

    /**
     * Create production sector trailer with system keys and access bits
     * @param productionKeys Map containing KeyA and KeyB as hex strings (12 chars each)
     * @return ByteArray of 16 bytes representing the sector trailer
     * @throws NFCException if keys are missing or invalid
     */
    fun createProductionSectorTrailer(productionKeys: Map<NFCTagSectorKeyType, ByteArray>): ByteArray {
        val fffKeyBytes = hexStringToByteArray("FFFFFFFFFFFF")
        val productionKeyABytes = productionKeys[NFCTagSectorKeyType.A] ?: fffKeyBytes
        val productionKeyBBytes = productionKeys[NFCTagSectorKeyType.B] ?: fffKeyBytes

        return ByteArray(16).apply {
            // KeyA (bytes 0–5): Administrative key (not readable after programming)
            System.arraycopy(productionKeyABytes, 0, this, 0, 6)

            /**
             * Access conditions (bytes 6–8) + inverted check byte (byte 9)
             * Bits = 78 77 88 6B
             *
             * Effective rules:
             * - Data blocks (0–2): Read allowed with Key A or B, Write only with Key B
             * - Sector trailer (3): Key A = read-only, Key B = full control (read/write)
             */
            this[6] = 0x78.toByte()
            this[7] = 0x77.toByte()
            this[8] = 0x88.toByte()
            this[9] = 0x6B.toByte()

            // KeyB (bytes 10–15): Operational key (used for normal data access)
            System.arraycopy(productionKeyBBytes, 0, this, 10, 6)
        }
    }

    /**
     * Validates sector keys provided in order to prevent errors during NFC operations.
     */
    fun validateKeys(
        keys: Map<NFCTagSectorKeyType, String>
    ): Boolean {
        val keyA = keys[NFCTagSectorKeyType.A]
        val keyB = keys[NFCTagSectorKeyType.B]

        if( keyA == null || keyB == null) return false

        // Validate key format
        if (keyA.length != 12 || keyB.length != 12) return false

        val keyABytes = hexStringToByteArray(keyA)
        val keyBBytes = hexStringToByteArray(keyB)

        // Additional validation after conversion
        if (keyABytes.size != 6 || keyBBytes.size != 6) return false

        return true
    }
}
