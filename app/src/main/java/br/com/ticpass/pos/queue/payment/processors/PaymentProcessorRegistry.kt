package br.com.ticpass.pos.queue.payment.processors

/**
 * Payment Processor Registry
 * Singleton registry for payment processor instances
 * Ensures processors are only instantiated once and provides a clear place for processor registration
 */
object PaymentProcessorRegistry {
    // Processor instances (created lazily)
    private val acquirerProcessor by lazy { AcquirerPaymentProcessor() }
    private val cashProcessor by lazy { CashPaymentProcessor() }
    private val bitcoinLNProcessor by lazy { BitcoinLNPaymentProcessor() }
    private val transactionlessProcessor by lazy { TransactionlessProcessor() }
    private val personalPIXProcessor by lazy { PersonalPIXPaymentProcessor() }

    // Map of processor types to processors (for dynamic processor)
    private val processorMap: Map<PaymentProcessorType, PaymentProcessorBase> by lazy {
        mapOf(
            PaymentProcessorType.ACQUIRER to acquirerProcessor,
            PaymentProcessorType.CASH to cashProcessor,
            PaymentProcessorType.LN_BITCOIN to bitcoinLNProcessor,
            PaymentProcessorType.TRANSACTIONLESS to transactionlessProcessor,
            PaymentProcessorType.PERSONAL_PIX to personalPIXProcessor
        )
    }

    // Create a dynamic processor with all registered processors
    fun createDynamicProcessor(): DynamicPaymentProcessor {
        return DynamicPaymentProcessor(processorMap)
    }
}
