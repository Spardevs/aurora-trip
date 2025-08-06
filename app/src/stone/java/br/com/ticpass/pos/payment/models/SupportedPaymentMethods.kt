package br.com.ticpass.pos.payment.models

/**
 * Stone supported payment methods
 */
object SupportedPaymentMethods {
    val methods = listOf(
        SystemPaymentMethod.CREDIT,
        SystemPaymentMethod.DEBIT,
        SystemPaymentMethod.VOUCHER,
        SystemPaymentMethod.PIX,
        SystemPaymentMethod.CASH,
        SystemPaymentMethod.LN_BITCOIN,
        SystemPaymentMethod.MERCHANT_PIX,
    )
}