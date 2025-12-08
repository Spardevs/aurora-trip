package br.com.ticpass.pos.presentation.nfc.models

/**
 * Enum for MIFARE Classic card models
 */
enum class NFCTagModel(val chipType: Int, val displayName: String, val sectorCount: Int, val totalMemory: Int) {
    CLASSIC_1K(0x31, "MIFARE Classic 1K", 16, 752), // 752 bytes useful data
    CLASSIC_2K(0x32, "MIFARE Classic 2K", 32, 1504), // 1504 bytes useful data
    CLASSIC_4K(0x34, "MIFARE Classic 4K", 40, 3440), // 3440 bytes useful data
    UNKNOWN(-1, "MIFARE Classic (Unknown)", 0, 0);

    override fun toString(): String = displayName

    companion object {
        fun fromChipType(chipType: Int): NFCTagModel {
            return values().find { it.chipType == chipType } ?: UNKNOWN
        }

        fun fromSectorCount(sectorCount: Int): NFCTagModel {
            return when {
                sectorCount <= 16 -> CLASSIC_1K
                sectorCount <= 32 -> CLASSIC_2K
                sectorCount <= 40 -> CLASSIC_4K
                else -> UNKNOWN
            }
        }
    }
}
