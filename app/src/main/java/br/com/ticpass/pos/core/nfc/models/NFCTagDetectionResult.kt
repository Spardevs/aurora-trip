package br.com.ticpass.pos.core.nfc.models

/**
 * Result of card detection containing UUID and provider info
 */
data class NFCTagDetectionResult(
    val cardUUID: ByteArray,
    val cardUUIDString: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as NFCTagDetectionResult
        
        if (!cardUUID.contentEquals(other.cardUUID)) return false
        if (cardUUIDString != other.cardUUIDString) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = cardUUID.contentHashCode()
        result = 31 * result + cardUUIDString.hashCode()
        return result
    }
}
