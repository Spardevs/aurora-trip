package br.com.ticpass.pos.core.nfc.models

/**
 * Data class representing a sector with its blocks, keys, and memory usage
 */
data class NFCTagSectorData(
    val blocks: List<ByteArray>,
    val keyA: ByteArray?,
    val keyB: ByteArray?,
    val accessBits: ByteArray,
    val gpb: ByteArray, // General Purpose Byte
    val totalMemoryBytes: Int,
    val usedMemoryBytes: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NFCTagSectorData

        if (blocks.size != other.blocks.size) return false
        if (!blocks.zip(other.blocks).all { (a, b) -> a.contentEquals(b) }) return false
        if (keyA != null && other.keyA != null && !keyA.contentEquals(other.keyA)) return false
        if (keyA == null && other.keyA != null) return false
        if (keyA != null && other.keyA == null) return false
        if (keyB != null && other.keyB != null && !keyB.contentEquals(other.keyB)) return false
        if (keyB == null && other.keyB != null) return false
        if (keyB != null && other.keyB == null) return false
        if (!accessBits.contentEquals(other.accessBits)) return false
        if (!gpb.contentEquals(other.gpb)) return false
        if (totalMemoryBytes != other.totalMemoryBytes) return false
        if (usedMemoryBytes != other.usedMemoryBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blocks.map { it.contentHashCode() }.hashCode()
        result = 31 * result + (keyA?.contentHashCode() ?: 0)
        result = 31 * result + (keyB?.contentHashCode() ?: 0)
        result = 31 * result + accessBits.contentHashCode()
        result = 31 * result + gpb.contentHashCode()
        result = 31 * result + totalMemoryBytes
        result = 31 * result + usedMemoryBytes
        return result
    }
}
