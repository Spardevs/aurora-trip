package br.com.ticpass.pos.nfc.models

/**
 * Enum representing different types of data that can be stored on NFC tags
 */
enum class NFCTagDataType(val id: Byte) {
    CUSTOMER(0x01),
    CONFIGURATION(0x02),
    SYSTEM(0x03),
    TEMPORARY(0x04);

    companion object {
        fun fromId(id: Byte): NFCTagDataType? {
            return values().find { it.id == id }
        }
    }
}
