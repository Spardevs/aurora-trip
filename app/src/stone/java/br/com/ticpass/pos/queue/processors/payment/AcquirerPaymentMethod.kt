package br.com.ticpass.pos.queue.processors.payment

import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import stone.application.enums.TypeOfTransactionEnum

enum class AcquirerPaymentMethod(val key: SystemPaymentMethod, val code: TypeOfTransactionEnum) {
    VOUCHER(
        SystemPaymentMethod.VOUCHER,
        TypeOfTransactionEnum.VOUCHER
    ),
    DEBIT(
        SystemPaymentMethod.DEBIT,
        TypeOfTransactionEnum.DEBIT
    ),
    CREDIT(
        SystemPaymentMethod.CREDIT,
        TypeOfTransactionEnum.CREDIT
    ),
    PIX(
        SystemPaymentMethod.PIX,
        TypeOfTransactionEnum.PIX
    );

    companion object {
        fun translate(key: SystemPaymentMethod): TypeOfTransactionEnum {
            val code = entries.find { it.key == key }?.code ?: TypeOfTransactionEnum.PIX

            return code
        }

        fun translate(code: TypeOfTransactionEnum): SystemPaymentMethod {
            val key = entries.find { it.code == code }?.key ?: SystemPaymentMethod.PIX

            return key
        }
    }
}