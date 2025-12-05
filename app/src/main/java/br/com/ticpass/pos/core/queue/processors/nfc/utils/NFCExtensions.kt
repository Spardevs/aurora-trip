package br.com.ticpass.pos.core.queue.processors.nfc.utils

import br.com.ticpass.pos.core.queue.core.QueueItemStatus
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCBruteForce
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueEntity
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.core.queue.processors.nfc.processors.models.NFCProcessorType

/**
 * Extension functions for converting between NFCQueueEntity and NFCQueueItem
 */

/**
 * Convert NFCQueueItem to NFCQueueEntity
 */
fun NFCQueueItem.toEntity(): NFCQueueEntity {
    return when (this) {
        is NFCQueueItem.CustomerAuthOperation -> NFCQueueEntity(
            id = id,
            priority = priority,
            status = status.toString(),
            processorType = NFCProcessorType.CUSTOMER_AUTH,
        )
        is NFCQueueItem.TagFormatOperation -> NFCQueueEntity(
            id = id,
            priority = priority,
            status = status.toString(),
            processorType = NFCProcessorType.TAG_FORMAT,
            bruteForce = bruteForce,
        )
        is NFCQueueItem.CustomerSetupOperation -> NFCQueueEntity(
            id = id,
            priority = priority,
            status = status.toString(),
            processorType = NFCProcessorType.CUSTOMER_SETUP,
            timeout = timeout
        )

        is NFCQueueItem.CartReadOperation -> NFCQueueEntity(
            id = id,
            priority = priority,
            status = status.toString(),
            processorType = NFCProcessorType.CART_READ,
        )

        is NFCQueueItem.CartUpdateOperation -> NFCQueueEntity(
            id = id,
            priority = priority,
            status = status.toString(),
            processorType = NFCProcessorType.CART_UPDATE,
        )
    }
}

/**
 * Convert NFCQueueEntity to NFCQueueItem
 */
fun NFCQueueEntity.toQueueItem(): NFCQueueItem {
    val queueStatus = QueueItemStatus.valueOf(status.uppercase())
    
    return when (processorType) {
        NFCProcessorType.CUSTOMER_AUTH -> NFCQueueItem.CustomerAuthOperation(
            id = id,
            priority = priority,
            status = queueStatus,
        )

        NFCProcessorType.TAG_FORMAT -> NFCQueueItem.TagFormatOperation(
            id = id,
            priority = priority,
            status = queueStatus,
            bruteForce = bruteForce ?: NFCBruteForce.MOST_LIKELY,
        )

        NFCProcessorType.CUSTOMER_SETUP -> NFCQueueItem.CustomerSetupOperation(
            id = id,
            priority = priority,
            status = queueStatus,
            timeout = timeout ?: 30000L
        )

        NFCProcessorType.CART_READ -> NFCQueueItem.CustomerSetupOperation(
            id = id,
            priority = priority,
            status = queueStatus,
            timeout = timeout ?: 30000L
        )

        NFCProcessorType.CART_UPDATE -> NFCQueueItem.CustomerSetupOperation(
            id = id,
            priority = priority,
            status = queueStatus,
            timeout = timeout ?: 30000L
        )
    }
}
