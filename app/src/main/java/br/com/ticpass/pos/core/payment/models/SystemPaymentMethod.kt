package br.com.ticpass.pos.core.payment.models

enum class SystemPaymentMethod(
    val value: String,
) {
    CREDIT("credit"),
    DEBIT("debit"),
    VOUCHER("voucher"),
    PIX("pix"),
    MERCHANT_PIX("personal_pix"),
    CASH("cash"),
    LN_BITCOIN("ln_bitcoin");

    companion object {
        fun fromValue(value: String): SystemPaymentMethod {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown payment method: $value")
        }
    }
}