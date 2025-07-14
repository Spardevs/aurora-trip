package br.com.ticpass.pos.queue

/**
 * Persistence Strategy
 * Defines how and when queue items are persisted
 */
enum class PersistenceStrategy {
    IMMEDIATE,      // Save every item immediately to database
    ON_BACKGROUND,  // Save only when app goes to background
    NEVER           // Memory only (lose data on app kill)
}
