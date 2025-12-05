package br.com.ticpass.pos.queue.processors.payment.processors

import br.com.ticpass.pos.queue.processors.payment.processors.core.DynamicPaymentProcessor
import br.com.ticpass.pos.queue.processors.payment.processors.core.PaymentProcessorBase
import br.com.ticpass.pos.queue.processors.payment.processors.impl.AcquirerPaymentProcessor
import br.com.ticpass.pos.queue.processors.payment.processors.impl.BitcoinLNPaymentProcessor
import br.com.ticpass.pos.queue.processors.payment.processors.impl.CashPaymentProcessor
import br.com.ticpass.pos.queue.processors.payment.processors.impl.MerchantPIXPaymentProcessor
import br.com.ticpass.pos.queue.processors.payment.processors.impl.TransactionlessProcessor
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Payment Processor Registry
 * Injectable registry for payment processor instances.
 * Uses Hilt to inject processor dependencies.
 */
@Singleton
class PaymentProcessorRegistry @Inject constructor(
    private val acquirerProcessor: AcquirerPaymentProcessor,
    private val cashProcessor: CashPaymentProcessor,
    private val bitcoinLNProcessor: BitcoinLNPaymentProcessor,
    private val transactionlessProcessor: TransactionlessProcessor,
    private val personalPIXProcessor: MerchantPIXPaymentProcessor
) {
    // Map of processor types to processors (for dynamic processor)
    private val processorMap: Map<PaymentProcessorType, PaymentProcessorBase> by lazy {
        mapOf(
            PaymentProcessorType.ACQUIRER to acquirerProcessor,
            PaymentProcessorType.CASH to cashProcessor,
            PaymentProcessorType.LN_BITCOIN to bitcoinLNProcessor,
            PaymentProcessorType.TRANSACTIONLESS to transactionlessProcessor,
            PaymentProcessorType.MERCHANT_PIX to personalPIXProcessor
        )
    }

    // Create a dynamic processor with all registered processors
    fun createDynamicProcessor(): DynamicPaymentProcessor {
        return DynamicPaymentProcessor(processorMap)
    }
}
