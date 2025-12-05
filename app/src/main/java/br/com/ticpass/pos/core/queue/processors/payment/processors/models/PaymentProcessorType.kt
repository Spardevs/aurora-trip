package br.com.ticpass.pos.core.queue.processors.payment.processors.models

/**
 * Enum defining the types of payment processors available in the system
 */
enum class PaymentProcessorType {
    ACQUIRER,
    CASH,
    LN_BITCOIN,
    TRANSACTIONLESS,
    MERCHANT_PIX
}
