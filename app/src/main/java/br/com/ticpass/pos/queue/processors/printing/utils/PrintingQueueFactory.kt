package br.com.ticpass.pos.queue.processors.printing.utils

import br.com.ticpass.pos.queue.config.PersistenceStrategy
import br.com.ticpass.pos.queue.config.ProcessorStartMode
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.printing.data.PrintingStorage
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.PrintingProcessorRegistry
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Printing Queue Factory
 * Injectable factory class to create configured printing queue instances.
 */
@Singleton
class PrintingQueueFactory @Inject constructor(
    private val printingProcessorRegistry: PrintingProcessorRegistry
) {
    
    /**
     * Create a printing queue that can handle multiple printing types in a single queue
     * Uses a DynamicPrintingProcessor that delegates to the appropriate processor based on the item's processorType
     * 
     * @param storage The printing storage to use
     * @param persistenceStrategy The persistence strategy to use
     * @param scope The coroutine scope to use
     * @return A configured HybridQueueManager with a DynamicPrintingProcessor
     */
    fun createDynamicPrintingQueue(
        storage: PrintingStorage,
        persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode: ProcessorStartMode = ProcessorStartMode.IMMEDIATE,
        scope: CoroutineScope
    ): HybridQueueManager<PrintingQueueItem, PrintingEvent> {
        // Get a dynamic processor from the registry
        val dynamicProcessor = printingProcessorRegistry.createDynamicProcessor()
        
        return HybridQueueManager(
            storage = storage,
            processor = dynamicProcessor,
            persistenceStrategy = persistenceStrategy,
            startMode = startMode,
            scope = scope
        )
    }
}
