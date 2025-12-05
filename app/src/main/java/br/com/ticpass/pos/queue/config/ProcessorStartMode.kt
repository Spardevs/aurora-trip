package br.com.ticpass.pos.queue.config

/**
 * Queue Confirmation Mode
 * Defines how the queue manager should handle transitions between processors
 */
enum class ProcessorStartMode {
    /**
     * Immediately proceed to the start processor without waiting for confirmation
     */
    IMMEDIATE,

    /**
     * Await for confirmation before proceeding to the next processor
     */
    CONFIRMATION
}
