package br.com.ticpass.pos.printing.models

enum class SystemPrintingMethod(
    val value: String,
) {
    ACQUIRER("acquirer"),
    MP_4200_HS("mp4200HS"),
    MPT_II("mptII");

    companion object {
        fun fromValue(value: String): SystemPrintingMethod {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown printing method: $value")
        }
    }
}