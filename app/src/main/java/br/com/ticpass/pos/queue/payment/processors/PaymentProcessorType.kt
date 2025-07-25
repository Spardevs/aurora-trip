package br.com.ticpass.pos.queue.payment.processors

/**
 * Enum defining the types of payment processors available in the system
 */
enum class PaymentProcessorType {
    ACQUIRER,
    CASH,
    LN_BITCOIN,
    TRANSACTIONLESS,
    PERSONAL_PIX
}
