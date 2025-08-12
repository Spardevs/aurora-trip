package br.com.ticpass.pos.queue.models

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent

/**
 * Processing Result
 * Represents the possible outcomes when processing a queue item
 */
sealed class ProcessingResult {
    abstract class Success : ProcessingResult()
    abstract class Error(val event: ProcessingErrorEvent) : ProcessingResult()
}