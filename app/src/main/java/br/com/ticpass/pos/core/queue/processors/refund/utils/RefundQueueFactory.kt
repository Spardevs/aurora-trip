package br.com.ticpass.pos.core.queue.processors.refund.utils

import br.com.ticpass.pos.core.queue.config.PersistenceStrategy
import br.com.ticpass.pos.core.queue.config.ProcessorStartMode
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.processors.refund.data.RefundStorage
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.core.queue.processors.refund.processors.RefundProcessorRegistry
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refund Queue Factory
 * Injectable factory class to create configured refund queue instances.
 */
@Singleton
class RefundQueueFactory @Inject constructor(
    private val refundProcessorRegistry: RefundProcessorRegistry
) {
    
    /**
     * Create a refund queue that can handle multiple refund types in a single queue
     * Uses a DynamicRefundProcessor that delegates to the appropriate processor based on the item's processorType
     * 
     * @param storage The refund storage to use
     * @param persistenceStrategy The persistence strategy to use
     * @param scope The coroutine scope to use
     * @return A configured HybridQueueManager with a DynamicRefundProcessor
     */
    fun createDynamicRefundQueue(
        storage: RefundStorage,
        persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode: ProcessorStartMode = ProcessorStartMode.IMMEDIATE,
        scope: CoroutineScope
    ): HybridQueueManager<RefundQueueItem, RefundEvent> {
        // Get a dynamic processor from the registry
        val dynamicProcessor = refundProcessorRegistry.createDynamicProcessor()
        
        return HybridQueueManager(
            storage = storage,
            processor = dynamicProcessor,
            persistenceStrategy = persistenceStrategy,
            startMode = startMode,
            scope = scope
        )
    }
}
