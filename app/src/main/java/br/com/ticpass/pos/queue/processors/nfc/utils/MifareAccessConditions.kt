package br.com.ticpass.pos.queue.processors.nfc.utils

/**
 * MIFARE Classic access conditions parser and data structures
 * 
 * This utility provides comprehensive parsing and interpretation of MIFARE Classic
 * access bits according to the official specification. It can determine which keys
 * (A, B, both, or never) are required for different operations on data blocks and
 * sector trailers.
 * 
 * Usage:
 * ```kotlin
 * val accessBits = byteArrayOf(0x77, 0x87, 0x78) // From sector trailer bytes 6,7,8
 * val conditions = MifareAccessConditions.parse(accessBits)
 * 
 * // Check what key is needed for writing to block 1
 * val writeKey = conditions.block1.writeKey // Returns KeyType.B, KeyType.A, KeyType.BOTH, or KeyType.NEVER
 * ```
 */
data class MifareAccessConditions(
    val block0: BlockAccess,
    val block1: BlockAccess, 
    val block2: BlockAccess,
    val trailerBlock: TrailerAccess
) {
    
    /**
     * Access permissions for a data block
     */
    data class BlockAccess(
        val readKey: KeyType?,
        val writeKey: KeyType?,
        val incrementKey: KeyType?,
        val decrementKey: KeyType?
    )
    
    /**
     * Access permissions for the sector trailer block
     */
    data class TrailerAccess(
        val keyARead: KeyType?,
        val keyAWrite: KeyType?, 
        val accessBitsRead: KeyType?,
        val accessBitsWrite: KeyType?,
        val keyBRead: KeyType?,
        val keyBWrite: KeyType?
    )
    
    /**
     * Key types that can perform operations
     */
    enum class KeyType { 
        A,      // Only Key A can perform this operation
        B,      // Only Key B can perform this operation
        BOTH,   // Both Key A and Key B can perform this operation
        NEVER   // This operation is never allowed
    }
    
    companion object {
        
        /**
         * Parse MIFARE Classic access conditions from raw access bits
         * 
         * @param accessBits The 3-byte access bits array from sector trailer bytes 6,7,8
         * @return Parsed access conditions for all blocks in the sector
         */
        fun parse(accessBits: ByteArray): MifareAccessConditions {
            require(accessBits.size == 3) { "Access bits must be exactly 3 bytes" }
            
            val c1Bits = extractC1Bits(accessBits)
            val c2Bits = extractC2Bits(accessBits) 
            val c3Bits = extractC3Bits(accessBits)
            
            return MifareAccessConditions(
                block0 = getDataBlockAccess(c1Bits[0], c2Bits[0], c3Bits[0]),
                block1 = getDataBlockAccess(c1Bits[1], c2Bits[1], c3Bits[1]),
                block2 = getDataBlockAccess(c1Bits[2], c2Bits[2], c3Bits[2]),
                trailerBlock = getTrailerAccess(c1Bits[3], c2Bits[3], c3Bits[3])
            )
        }
        
        /**
         * Extract C1 bits from access bit bytes according to MIFARE specification
         */
        private fun extractC1Bits(bytes: ByteArray): BooleanArray {
            val b7 = bytes[1].toInt() and 0xFF
            return booleanArrayOf(
                (b7 and 0x10) != 0, // C1_0
                (b7 and 0x20) != 0, // C1_1  
                (b7 and 0x40) != 0, // C1_2
                (b7 and 0x80) != 0  // C1_3
            )
        }
        
        /**
         * Extract C2 bits from access bit bytes according to MIFARE specification
         */
        private fun extractC2Bits(bytes: ByteArray): BooleanArray {
            val b8 = bytes[2].toInt() and 0xFF
            return booleanArrayOf(
                (b8 and 0x01) != 0, // C2_0
                (b8 and 0x02) != 0, // C2_1
                (b8 and 0x04) != 0, // C2_2  
                (b8 and 0x08) != 0  // C2_3
            )
        }
        
        /**
         * Extract C3 bits from access bit bytes according to MIFARE specification
         */
        private fun extractC3Bits(bytes: ByteArray): BooleanArray {
            val b8 = bytes[2].toInt() and 0xFF
            return booleanArrayOf(
                (b8 and 0x10) != 0, // C3_0
                (b8 and 0x20) != 0, // C3_1
                (b8 and 0x40) != 0, // C3_2
                (b8 and 0x80) != 0  // C3_3  
            )
        }
        
        /**
         * Get access permissions for a data block based on C1, C2, C3 bits
         * According to MIFARE Classic specification table
         */
        private fun getDataBlockAccess(c1: Boolean, c2: Boolean, c3: Boolean): BlockAccess {
            return when(Triple(c1, c2, c3)) {
                Triple(false, false, false) -> BlockAccess(KeyType.BOTH, KeyType.BOTH, KeyType.BOTH, KeyType.BOTH)
                Triple(false, true, false) -> BlockAccess(KeyType.BOTH, KeyType.NEVER, KeyType.NEVER, KeyType.NEVER)  
                Triple(true, false, false) -> BlockAccess(KeyType.BOTH, KeyType.B, KeyType.NEVER, KeyType.NEVER)
                Triple(true, true, false) -> BlockAccess(KeyType.BOTH, KeyType.B, KeyType.B, KeyType.BOTH)
                Triple(false, false, true) -> BlockAccess(KeyType.BOTH, KeyType.NEVER, KeyType.NEVER, KeyType.BOTH)
                Triple(false, true, true) -> BlockAccess(KeyType.B, KeyType.NEVER, KeyType.NEVER, KeyType.NEVER)
                Triple(true, false, true) -> BlockAccess(KeyType.B, KeyType.NEVER, KeyType.NEVER, KeyType.NEVER)
                else -> BlockAccess(KeyType.NEVER, KeyType.NEVER, KeyType.NEVER, KeyType.NEVER)
            }
        }
        
        /**
         * Get access permissions for the sector trailer based on C1, C2, C3 bits
         * According to MIFARE Classic specification table
         */
        private fun getTrailerAccess(c1: Boolean, c2: Boolean, c3: Boolean): TrailerAccess {
            return when(Triple(c1, c2, c3)) {
                Triple(false, false, false) -> TrailerAccess(
                    keyARead = KeyType.NEVER, keyAWrite = KeyType.A,
                    accessBitsRead = KeyType.A, accessBitsWrite = KeyType.NEVER,
                    keyBRead = KeyType.A, keyBWrite = KeyType.A
                )
                Triple(false, true, false) -> TrailerAccess(
                    keyARead = KeyType.NEVER, keyAWrite = KeyType.NEVER,
                    accessBitsRead = KeyType.A, accessBitsWrite = KeyType.A, 
                    keyBRead = KeyType.A, keyBWrite = KeyType.NEVER
                )
                Triple(true, false, false) -> TrailerAccess(
                    keyARead = KeyType.NEVER, keyAWrite = KeyType.B,
                    accessBitsRead = KeyType.A, accessBitsWrite = KeyType.B,
                    keyBRead = KeyType.NEVER, keyBWrite = KeyType.B
                )
                Triple(true, true, false) -> TrailerAccess(
                    keyARead = KeyType.NEVER, keyAWrite = KeyType.NEVER,
                    accessBitsRead = KeyType.A, accessBitsWrite = KeyType.A,
                    keyBRead = KeyType.NEVER, keyBWrite = KeyType.NEVER
                )
                Triple(false, false, true) -> TrailerAccess(
                    keyARead = KeyType.NEVER, keyAWrite = KeyType.A,
                    accessBitsRead = KeyType.A, accessBitsWrite = KeyType.A,
                    keyBRead = KeyType.A, keyBWrite = KeyType.A
                )
                Triple(false, true, true) -> TrailerAccess(
                    keyARead = KeyType.NEVER, keyAWrite = KeyType.B,
                    accessBitsRead = KeyType.A, accessBitsWrite = KeyType.B,
                    keyBRead = KeyType.NEVER, keyBWrite = KeyType.B
                )
                Triple(true, false, true) -> TrailerAccess(
                    keyARead = KeyType.NEVER, keyAWrite = KeyType.NEVER,
                    accessBitsRead = KeyType.A, accessBitsWrite = KeyType.B,
                    keyBRead = KeyType.NEVER, keyBWrite = KeyType.NEVER
                )
                else -> TrailerAccess(
                    keyARead = KeyType.NEVER, keyAWrite = KeyType.NEVER,
                    accessBitsRead = KeyType.A, accessBitsWrite = KeyType.NEVER,
                    keyBRead = KeyType.NEVER, keyBWrite = KeyType.NEVER
                )
            }
        }
    }
}
