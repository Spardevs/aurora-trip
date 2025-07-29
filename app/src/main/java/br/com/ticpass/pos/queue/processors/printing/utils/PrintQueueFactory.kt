package br.com.ticpass.pos.queue.processors.printing.utils

import br.com.ticpass.pos.queue.config.PersistenceStrategy
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.printing.data.PrintStorage
import br.com.ticpass.pos.queue.processors.printing.models.PrintQueueItem
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.queue.processors.printing.processors.core.PrintProcessor
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
