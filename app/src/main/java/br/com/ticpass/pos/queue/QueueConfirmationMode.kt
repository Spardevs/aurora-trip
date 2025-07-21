package br.com.ticpass.pos.queue

/**
 * Queue Confirmation Mode
 * Defines how the queue manager should handle transitions between processors
 */
enum class QueueConfirmationMode {
    /**
     * Automatically proceed to the next processor without user confirmation
     */
    AUTO,

    /**
     * Request user confirmation before proceeding to the next processor
     */
    CONFIRMATION
}
