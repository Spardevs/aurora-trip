package br.com.ticpass.pos.queue.processors.payment.utils

import br.com.ticpass.pos.queue.config.PersistenceStrategy
import br.com.ticpass.pos.queue.config.ProcessorStartMode
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.payment.data.PaymentProcessingStorage
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.queue.processors.payment.processors.PaymentProcessorRegistry
import kotlinx.coroutines.CoroutineScope

/**
 * Payment Queue Factory
 * Helper class to create configured payment queue instances
 */
class PaymentProcessingQueueFactory {
    
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
        storage: PaymentProcessingStorage,
        persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode: ProcessorStartMode = ProcessorStartMode.IMMEDIATE,
        scope: CoroutineScope
    ): HybridQueueManager<PaymentProcessingQueueItem, PaymentProcessingEvent> {
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
