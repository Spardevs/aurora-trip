package br.com.ticpass.pos.queue.printing

import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.PersistenceStrategy
import kotlinx.coroutines.CoroutineScope

/**
 * Print Queue Factory
 * Helper class to create configured print queue instances
 */
class PrintQueueFactory {
    /**
     * Create a print queue with the specified storage, persistence strategy, and scope
     * Returns a HybridQueueManager with strongly-typed PrintingEvent support
     */
    fun createPrintQueue(
        storage: PrintStorage,
        persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
        scope: CoroutineScope
    ): HybridQueueManager<PrintQueueItem, PrintingEvent> {
        return HybridQueueManager(
            storage = storage,
            processor = PrintProcessor(),
            persistenceStrategy = persistenceStrategy,
            scope = scope
        )
    }
}
