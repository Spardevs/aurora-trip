package br.com.ticpass.pos.core.queue.config

/**
 * Persistence Strategy
 * Defines how and when queue items are persisted
 */
enum class PersistenceStrategy {
    /**
     *  Save every item immediately to database
     */
    IMMEDIATE,

    /**
     * Memory only (lose data on app kill)
     */
    NEVER
}
