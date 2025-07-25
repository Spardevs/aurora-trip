package br.com.ticpass.pos.queue.payment

import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.PersistenceStrategy
import br.com.ticpass.pos.queue.ProcessorStartMode
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorRegistry
import kotlinx.coroutines.CoroutineScope

/**
 * Payment Queue Factory
 * Helper class to create configured payment queue instances
 */
class ProcessingPaymentQueueFactory {
    
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
        // Get a dynamic processor from the registry
        val dynamicProcessor = PaymentProcessorRegistry.createDynamicProcessor()
        
        return HybridQueueManager(
            storage = storage,
            processor = dynamicProcessor,
            persistenceStrategy = persistenceStrategy,
            startMode = startMode,
            scope = scope
        )
    }
}
