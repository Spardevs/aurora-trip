package br.com.ticpass.pos.queue

/**
 * Processing Result
 * Represents the possible outcomes when processing a queue item
 */
sealed class ProcessingResult {
    object Success : ProcessingResult()
    data class Error(val message: String) : ProcessingResult()
    object Retry : ProcessingResult()
}
