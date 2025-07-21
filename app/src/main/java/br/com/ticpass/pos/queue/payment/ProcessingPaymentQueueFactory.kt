package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.QueueConfirmationMode
import br.com.ticpass.pos.queue.QueueProcessor
import br.com.ticpass.pos.queue.payment.processors.DynamicPaymentProcessor
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorBase
import kotlinx.coroutines.CoroutineScope

/**
 * Payment Queue Factory
 * Helper class to create configured payment queue instances
 */
class ProcessingPaymentQueueFactory {
    // Processor provider instance
    private val processorProvider = PaymentProcessorProvider()
    
    // Create processor instances (shared across methods)
    private val acquirerProcessor = processorProvider.getProcessor("acquirer") as PaymentProcessorBase
    private val cashProcessor = processorProvider.getProcessor("cash") as PaymentProcessorBase
    private val transactionlessProcessor = processorProvider.getProcessor("transactionless") as PaymentProcessorBase
    
    // Create a map of processor types to processors
    private val processorMap = mapOf(
        "acquirer" to acquirerProcessor,
        "cash" to cashProcessor,
        "transactionless" to transactionlessProcessor
    )
    
    /**
     * Create a payment queue with the specified storage, persistence strategy, and scope
     * Uses the specified payment method to select the appropriate processor
     * 
     * @param storage The payment storage to use
     * @param paymentMethod The payment method to use (e.g., "acquirer", "cash", "transactionless")
     * @param persistenceStrategy The persistence strategy to use
     * @param scope The coroutine scope to use
     * @return A configured HybridQueueManager with the appropriate processor
     */
    fun createPaymentQueue(
        storage: ProcessingPaymentStorage,
        paymentMethod: String = "acquirer",
        persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
        queueConfirmationMode: QueueConfirmationMode = QueueConfirmationMode.AUTO,
        scope: CoroutineScope
    ): HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent> {
        return HybridQueueManager(
            storage = storage,
            processor = processorProvider.getProcessor(paymentMethod),
            persistenceStrategy = persistenceStrategy,
            queueConfirmationMode = queueConfirmationMode,
            scope = scope
        )
    }
    
    /**
     * Create a payment queue with a specific processor
     * 
     * @param storage The payment storage to use
     * @param processor The specific processor to use
     * @param persistenceStrategy The persistence strategy to use
     * @param scope The coroutine scope to use
     * @return A configured HybridQueueManager with the specified processor
     */
    fun createPaymentQueueWithProcessor(
        storage: ProcessingPaymentStorage,
        processor: QueueProcessor<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
        queueConfirmationMode: QueueConfirmationMode = QueueConfirmationMode.AUTO,
        scope: CoroutineScope
    ): HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent> {
        return HybridQueueManager(
            storage = storage,
            processor = processor,
            persistenceStrategy = persistenceStrategy,
            queueConfirmationMode = queueConfirmationMode,
            scope = scope
        )
    }
    
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
        queueConfirmationMode: QueueConfirmationMode = QueueConfirmationMode.AUTO,
        scope: CoroutineScope
    ): HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent> {
        // Create a dynamic processor with all our processor types
        val dynamicProcessor = DynamicPaymentProcessor(processorMap)
        
        return HybridQueueManager(
            storage = storage,
            processor = dynamicProcessor,
            persistenceStrategy = persistenceStrategy,
            queueConfirmationMode = queueConfirmationMode,
            scope = scope
        )
    }
}
