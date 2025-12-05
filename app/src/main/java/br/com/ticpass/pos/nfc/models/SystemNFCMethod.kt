package br.com.ticpass.pos.nfc.models

enum class SystemNFCMethod(
    val value: String,
) {
    CUSTOMER_AUTH("customer_auth"),
    TAG_FORMAT("tag_format"),
    CUSTOMER_SETUP("customer_setup"),
    CART_READ("cart_read"),
    CART_UPDATE("cart_update");

    companion object {
        fun fromValue(value: String): SystemNFCMethod {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown nfc method: $value")
        }
    }
}