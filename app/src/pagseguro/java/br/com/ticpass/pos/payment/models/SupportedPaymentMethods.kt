package br.com.ticpass.pos.payment.models

/**
 * PagSeguro supported payment methods
 */
object SupportedPaymentMethods {
    val methods = listOf(
        SystemPaymentMethod.CREDIT,
        SystemPaymentMethod.DEBIT,
        SystemPaymentMethod.VOUCHER,
        SystemPaymentMethod.PIX,
        SystemPaymentMethod.CASH,
    )
}