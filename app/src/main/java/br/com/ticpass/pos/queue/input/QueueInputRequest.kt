package br.com.ticpass.pos.queue.input

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import java.util.UUID

/**
 * Queue Input Request
 * Represents a request for user input at the queue manager level
 * (as opposed to processor-level UserInputRequest)
 */
sealed class QueueInputRequest {
    abstract val id: String
    abstract val timeoutMs: Long?

    /**
     * Request for confirmation before proceeding to the next processor
     * Generic version without payment-specific details
     */
    data class CONFIRM_NEXT_PROCESSOR(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long? = 60_000L, // 60 seconds default timeout
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentItemId: String,
        val nextItemId: String?
    ) : QueueInputRequest()
    
    /**
     * Request for retry or skip when a processing error occurs
     */
    data class ERROR_RETRY_OR_SKIP(
        override val id: String = UUID.randomUUID().toString(),
        override val timeoutMs: Long? = 60_000L, // 60 seconds default timeout
        val itemId: String,
        val error: ProcessingErrorEvent
    ) : QueueInputRequest()
}
