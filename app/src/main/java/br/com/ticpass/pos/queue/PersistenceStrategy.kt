package br.com.ticpass.pos.queue

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
     * Save only when app goes to background
     */
    ON_BACKGROUND,

    /**
     * Memory only (lose data on app kill)
     */
    NEVER
}
