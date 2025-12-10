package br.com.ticpass.pos.core.nfc.models

/**
 * Data class to hold both A and B keys for a sector
 */
data class NFCTagSectorKeys(
    val typeA: String? = null,
    val typeB: String? = null
) {
    fun isComplete(): Boolean = typeA != null && typeB != null
    fun hasAnyKey(): Boolean = typeA != null || typeB != null
    
    override fun toString(): String {
        return "A=${typeA ?: "null"}, B=${typeB ?: "null"}"
    }
}
