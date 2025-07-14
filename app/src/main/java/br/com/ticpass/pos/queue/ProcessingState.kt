package br.com.ticpass.pos.queue

/**
 * Processing State
 * Represents the current state of a queue item being processed
 */
sealed class ProcessingState<T : QueueItem> {
    data class Idle<T : QueueItem>(val item: T) : ProcessingState<T>()
    data class Processing<T : QueueItem>(val item: T) : ProcessingState<T>()
    data class Completed<T : QueueItem>(val item: T) : ProcessingState<T>()
    data class Failed<T : QueueItem>(val item: T, val error: String) : ProcessingState<T>()
    data class Retrying<T : QueueItem>(val item: T) : ProcessingState<T>()
}
