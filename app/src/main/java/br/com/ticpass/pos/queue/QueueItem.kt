package br.com.ticpass.pos.queue

/**
 * Generic Queue Item Interface
 * Defines the core properties that any queue item must have
 */
interface QueueItem {
    val id: String
    val priority: Int
    val status: QueueItemStatus
}

enum class QueueItemStatus {
    PENDING, // Item is waiting to be processed
    PROCESSING, // Item is currently being processed
    COMPLETED, // Item has been processed successfully
    FAILED, // Item processing failed
    CANCELLED // Item processing was cancelled
}
