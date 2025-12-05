package br.com.ticpass.pos.queue.processors.nfc.utils

import br.com.ticpass.pos.queue.config.PersistenceStrategy
import br.com.ticpass.pos.queue.config.ProcessorStartMode
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.nfc.data.NFCStorage
import br.com.ticpass.pos.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.queue.processors.nfc.processors.NFCProcessorRegistry
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NFC Queue Factory
 * Injectable factory class to create configured NFC queue instances.
 */
@Singleton
class NFCQueueFactory @Inject constructor(
    private val nfcProcessorRegistry: NFCProcessorRegistry
) {
    
    /**
     * Create a NFC queue that can handle multiple NFC types in a single queue
     * Uses a DynamicNFCProcessor that delegates to the appropriate processor based on the item's processorType
     * 
     * @param storage The NFC storage to use
     * @param persistenceStrategy The persistence strategy to use
     * @param scope The coroutine scope to use
     * @return A configured HybridQueueManager with a DynamicNFCProcessor
     */
    fun createDynamicNFCQueue(
        storage: NFCStorage,
        persistenceStrategy: PersistenceStrategy = PersistenceStrategy.IMMEDIATE,
        startMode: ProcessorStartMode = ProcessorStartMode.IMMEDIATE,
        scope: CoroutineScope
    ): HybridQueueManager<NFCQueueItem, NFCEvent> {
        // Get a dynamic processor from the registry
        val dynamicProcessor = nfcProcessorRegistry.createDynamicProcessor()
        
        return HybridQueueManager(
            storage = storage,
            processor = dynamicProcessor,
            persistenceStrategy = persistenceStrategy,
            startMode = startMode,
            scope = scope
        )
    }
}
