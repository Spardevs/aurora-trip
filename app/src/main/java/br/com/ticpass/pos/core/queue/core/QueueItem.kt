package br.com.ticpass.pos.core.queue.core

/**
 * Generic Queue Item Interface
 * Defines the core properties that any queue item must have
 */
interface QueueItem {
    val id: String
    val priority: Int
    var status: QueueItemStatus
}

enum class QueueItemStatus {
    PENDING, // Item is waiting to be processed
    PROCESSING, // Item is currently being processed
    COMPLETED, // Item has been processed successfully
    FAILED, // Item processing failed
    CANCELLED // Item processing was cancelled
}
