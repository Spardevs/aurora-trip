package br.com.ticpass.pos.presentation.nfc.models

/**
 * Enum for NFC tag models that are supported by the application
 */
enum class NFCTagSupportedModels(val displayName: String, val chipType: Int, val sectorCount: Int) {
    MIFARE_CLASSIC_1K(
        NFCTagModel.CLASSIC_1K.displayName,
        NFCTagModel.CLASSIC_1K.chipType,
        NFCTagModel.CLASSIC_1K.sectorCount
    );

    override fun toString(): String = displayName

    companion object {
        /**
         * Check if a chip type is supported
         */
        fun isSupported(chipType: Int): Boolean {
            return values().any { it.chipType == chipType }
        }

        /**
         * Get supported model from chip type
         */
        fun fromChipType(chipType: Int): NFCTagSupportedModels? {
            return values().find { it.chipType == chipType }
        }

        /**
         * Get all supported models
         */
        fun getSupportedModels(): List<NFCTagSupportedModels> {
            return values().toList()
        }
    }
}
