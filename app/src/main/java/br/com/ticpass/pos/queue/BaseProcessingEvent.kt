package br.com.ticpass.pos.queue

/**
 * Generic Queue Item Interface
 * Defines the core properties that any queue item must have
 */
interface BaseProcessingEvent {
    val itemId: String
}