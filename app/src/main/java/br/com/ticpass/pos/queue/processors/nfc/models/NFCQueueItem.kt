package br.com.ticpass.pos.queue.processors.nfc.models

import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.processors.nfc.processors.models.NFCProcessorType
import java.util.UUID

/**
 * NFC Queue Item
 * Represents a nfc operation in the queue
 */
sealed class NFCQueueItem : QueueItem {
    abstract val processorType: NFCProcessorType
    
    /**
     * NFC Auth operation with authentication-specific parameters
     */
    data class CustomerAuthOperation(
        override val id: String = UUID.randomUUID().toString(),
        override val priority: Int = 0,
        override var status: QueueItemStatus = QueueItemStatus.PENDING,
        override val processorType: NFCProcessorType = NFCProcessorType.CUSTOMER_AUTH,
        val timeout: Long = 15000L // Auth timeout in milliseconds
    ) : NFCQueueItem()

    /**
     * NFC Format operation with format-specific parameters
     */
    data class TagFormatOperation(
        override val id: String = UUID.randomUUID().toString(),
        override val priority: Int = 0,
        override var status: QueueItemStatus = QueueItemStatus.PENDING,
        override val processorType: NFCProcessorType = NFCProcessorType.TAG_FORMAT,
        val bruteForce: NFCBruteForce,
    ) : NFCQueueItem()
    
    /**
     * NFC Setup operation with setup-specific parameters
     */
    data class CustomerSetupOperation(
        override val id: String = UUID.randomUUID().toString(),
        override val priority: Int = 0,
        override var status: QueueItemStatus = QueueItemStatus.PENDING,
        override val processorType: NFCProcessorType = NFCProcessorType.CUSTOMER_SETUP,
        val timeout: Long = 20000L // Setup timeout in milliseconds
    ) : NFCQueueItem()
}
