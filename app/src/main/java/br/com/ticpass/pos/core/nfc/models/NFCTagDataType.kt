package br.com.ticpass.pos.core.nfc.models

/**
 * Enum representing different types of data that can be stored on NFC tags
 */
enum class NFCTagDataType(val id: Byte) {
    CUSTOMER(0x01),
    CART(0x02),
    BALANCE(0x03),
    CONFIGURATION(0x04),
    SYSTEM(0x05),
    TEMPORARY(0x06);

    companion object {
        fun fromId(id: Byte): NFCTagDataType? {
            return values().find { it.id == id }
        }
    }
}
