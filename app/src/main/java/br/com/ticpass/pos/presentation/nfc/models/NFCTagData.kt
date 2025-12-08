package br.com.ticpass.pos.presentation.nfc.models

/**
 * Data class representing complete NFC tag information
 */
data class NFCTagData(
    val manufacturer: NFCTagManufacturer,
    val uid: ByteArray,
    val bcc: ByteArray,
    val model: NFCTagModel,
    val sectors: List<NFCTagSectorData>,
    val totalMemoryBytes: Int,
    val usedMemoryBytes: Int
) {
    /**
     * Formats the tag data as a readable string with complete information
     */
    override fun toString(): String {
        val builder = StringBuilder()
        
        // Header with card information
        builder.append("═══════════════════════════════════════\n")
        builder.append("NFC TAG INFORMATION\n")
        builder.append("═══════════════════════════════════════\n")
        builder.append("Manufacturer: ${manufacturer.displayName}\n")
        builder.append("Model: ${model.displayName}\n")
        builder.append("UID: ${uid.joinToString("") { "%02X".format(it) }}\n")
        builder.append("BCC: ${bcc.joinToString("") { "%02X".format(it) }}\n")
        builder.append("Total Memory: $totalMemoryBytes bytes\n")
        builder.append("Used Memory: $usedMemoryBytes bytes\n")
        builder.append("Free Memory: ${totalMemoryBytes - usedMemoryBytes} bytes\n")
        builder.append("═══════════════════════════════════════\n\n")
        
        // Sector data
        sectors.forEachIndexed { index, sector ->
            builder.append("Sector: $index\n")
            builder.append("Memory: ${sector.usedMemoryBytes}/${sector.totalMemoryBytes} bytes used\n\n")
            
            sector.blocks.forEachIndexed { blockIndex, block ->
                val blockHex = block.joinToString("") { "%02X".format(it) }
                
                when {
                    index == 0 && blockIndex == 0 -> {
                        // Manufacturer block - highlight UID and BCC
                        val uidHex = uid.joinToString("") { "%02X".format(it) }
                        val bccHex = bcc.joinToString("") { "%02X".format(it) }
                        val remainingHex = blockHex.substring((uid.size + bcc.size) * 2)
                        builder.append("$uidHex$bccHex$remainingHex\n")
                    }
                    blockIndex == 3 || (index >= 32 && blockIndex == 15) -> {
                        // Sector trailer - reconstruct with actual authentication keys used
                        val authKeyA = sector.keyA?.joinToString("") { "%02X".format(it) } ?: "UNKNOWN"
                        val accessBits = sector.accessBits.joinToString("") { "%02X".format(it) }
                        val gpb = sector.gpb.joinToString("") { "%02X".format(it) }
                        val authKeyB = sector.keyB?.joinToString("") { "%02X".format(it) } ?: "UNKNOWN"
                        builder.append("$authKeyA$accessBits$gpb$authKeyB\n")
                    }
                    else -> {
                        // Regular data block
                        builder.append("$blockHex\n")
                    }
                }
            }
            
            if (index < sectors.size - 1) {
                builder.append("\n")
            }
        }
        
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NFCTagData

        if (manufacturer != other.manufacturer) return false
        if (!uid.contentEquals(other.uid)) return false
        if (!bcc.contentEquals(other.bcc)) return false
        if (model != other.model) return false
        if (sectors != other.sectors) return false
        if (totalMemoryBytes != other.totalMemoryBytes) return false
        if (usedMemoryBytes != other.usedMemoryBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manufacturer.hashCode()
        result = 31 * result + uid.contentHashCode()
        result = 31 * result + bcc.contentHashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + sectors.hashCode()
        result = 31 * result + totalMemoryBytes
        result = 31 * result + usedMemoryBytes
        return result
    }
}
