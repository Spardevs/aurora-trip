package br.com.ticpass.pos.core.nfc.models

enum class SystemNFCMethod(
    val value: String,
) {
    CUSTOMER_AUTH("customer_auth"),
    TAG_FORMAT("tag_format"),
    CUSTOMER_SETUP("customer_setup"),
    CART_READ("cart_read"),
    CART_UPDATE("cart_update"),
    BALANCE_READ("balance_read"),
    BALANCE_SET("balance_set"),
    BALANCE_CLEAR("balance_clear");

    companion object {
        fun fromValue(value: String): SystemNFCMethod {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown nfc method: $value")
        }
    }
}