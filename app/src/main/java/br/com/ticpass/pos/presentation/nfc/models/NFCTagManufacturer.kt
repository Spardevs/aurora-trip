package br.com.ticpass.pos.presentation.nfc.models

/**
 * Enum for NFC card manufacturers
 */
enum class NFCTagManufacturer(val code: Int, val displayName: String) {
    NXP(0x04, "NXP"),
    ST_MICROELECTRONICS(0x02, "STMicroelectronics"),
    INFINEON(0x05, "Infineon"),
    CYPRESS(0x06, "Cypress"),
    TEXAS_INSTRUMENTS(0x07, "Texas Instruments"),
    FUJITSU(0x08, "Fujitsu"),
    MATSUSHITA(0x09, "Matsushita"),
    NEC(0x0A, "NEC"),
    OKI_ELECTRIC(0x0B, "Oki Electric"),
    TOSHIBA(0x0C, "Toshiba"),
    MITSUBISHI(0x0D, "Mitsubishi"),
    SAMSUNG(0x0E, "Samsung"),
    HYUNDAI(0x0F, "Hyundai"),
    UNKNOWN(-1, "Unknown");

    override fun toString(): String = displayName

    companion object {
        fun fromCode(code: Int): NFCTagManufacturer {
            return values().find { it.code == code } ?: UNKNOWN
        }
    }
}
