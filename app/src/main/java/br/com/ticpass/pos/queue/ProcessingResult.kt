package br.com.ticpass.pos.queue

/**
 * Processing Result
 * Represents the possible outcomes when processing a queue item
 */
sealed class ProcessingResult {
    object Retry : ProcessingResult()

    class Success(
        val atk: String,
        val txId: String,
    ) : ProcessingResult()

    data class Error(
        val event: ProcessingErrorEvent
    ) : ProcessingResult()
}
