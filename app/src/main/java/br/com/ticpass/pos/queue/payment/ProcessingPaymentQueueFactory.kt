package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessorStartMode
import br.com.ticpass.pos.queue.payment.processors.AcquirerPaymentProcessor
import br.com.ticpass.pos.queue.payment.processors.BitcoinLNPaymentProcessor
import br.com.ticpass.pos.queue.payment.processors.CashPaymentProcessor
import br.com.ticpass.pos.queue.payment.processors.DynamicPaymentProcessor
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorType
import br.com.ticpass.pos.queue.payment.processors.TransactionlessProcessor
import kotlinx.coroutines.CoroutineScope

/**
 * Payment Queue Factory
 * Helper class to create configured payment queue instances
 */
class ProcessingPaymentQueueFactory {
    // Create processor instances (shared across methods)
    private val acquirerProcessor = AcquirerPaymentProcessor()
    private val cashProcessor = CashPaymentProcessor()
    private val bitcoinLNProcessor = BitcoinLNPaymentProcessor()
    private val transactionlessProcessor = TransactionlessProcessor()

    // Create a map of processor types to processors
    private val processorMap = mapOf(
        PaymentProcessorType.ACQUIRER to acquirerProcessor,
        PaymentProcessorType.CASH to cashProcessor,
        PaymentProcessorType.LN_BITCOIN to bitcoinLNProcessor,
        PaymentProcessorType.TRANSACTIONLESS to transactionlessProcessor
    )
    
    /**
     * Create a payment queue that can handle multiple payment types in a single queue
     * Uses a DynamicPaymentProcessor that delegates to the appropriate processor based on the item's processorType
     * 
     * @param storage The payment storage to use
     * @param persistenceStrategy The persistence strategy to use
     * @param scope The coroutine scope to use
     * @return A configured HybridQueueManager with a DynamicPaymentProcessor
     */
    fun createDynamicPaymentQueue(
        storage: ProcessingPaymentStorage,
        persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode: ProcessorStartMode = ProcessorStartMode.IMMEDIATE,
        scope: CoroutineScope
    ): HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent> {
        // Create a dynamic processor with all our processor types
        val dynamicProcessor = DynamicPaymentProcessor(processorMap)
        
        return HybridQueueManager(
            storage = storage,
            processor = dynamicProcessor,
            persistenceStrategy = persistenceStrategy,
            startMode = startMode,
            scope = scope
        )
    }
}
