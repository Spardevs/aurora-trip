package br.com.ticpass.pos.queue.processors.nfc.models

import br.com.ticpass.pos.nfc.models.CartOperation
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
    
    /**
     * NFC Cart Read operation
     */
    data class CartReadOperation(
        override val id: String = UUID.randomUUID().toString(),
        override val priority: Int = 0,
        override var status: QueueItemStatus = QueueItemStatus.PENDING,
        override val processorType: NFCProcessorType = NFCProcessorType.CART_READ,
        val timeout: Long = 15000L // Read timeout in milliseconds
    ) : NFCQueueItem()
    
    /**
     * NFC Cart Update operation with cart-specific parameters
     */
    data class CartUpdateOperation(
        override val id: String = UUID.randomUUID().toString(),
        override val priority: Int = 0,
        override var status: QueueItemStatus = QueueItemStatus.PENDING,
        override val processorType: NFCProcessorType = NFCProcessorType.CART_UPDATE,
        val timeout: Long = 20000L, // Update timeout in milliseconds
        val productId: UShort,
        val quantity: UByte,
        val price: UInt, // Price per unit in cents when adding to cart
        val operation: CartOperation
    ) : NFCQueueItem()
}
