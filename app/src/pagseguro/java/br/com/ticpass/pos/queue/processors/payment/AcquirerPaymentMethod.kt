package br.com.ticpass.pos.queue.processors.payment

import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag

enum class AcquirerPaymentMethod(val key: SystemPaymentMethod, val code: Int) {
    VOUCHER(
        SystemPaymentMethod.VOUCHER,
        PlugPag.TYPE_VOUCHER
    ),
    DEBIT(
        SystemPaymentMethod.DEBIT,
        PlugPag.TYPE_DEBITO
    ),
    CREDIT(
        SystemPaymentMethod.CREDIT,
        PlugPag.TYPE_CREDITO
    ),
    PIX(
        SystemPaymentMethod.PIX,
        PlugPag.TYPE_PIX
    );

    companion object {
        fun translate(key: SystemPaymentMethod): Int {
            val code = entries.find { it.key == key }?.code ?: PlugPag.TYPE_PIX

            return code
        }

        fun translate(code: Int): SystemPaymentMethod {
            val key = entries.find { it.code == code }?.key ?: SystemPaymentMethod.PIX

            return key
        }
    }
}