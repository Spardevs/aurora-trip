package br.com.ticpass.pos.queue.core

import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.models.ProcessingResult
import kotlinx.coroutines.flow.SharedFlow

/**
 * Queue Processor
 * Processes queue items and returns results
 */
interface QueueProcessor<T : QueueItem, E : BaseProcessingEvent> {
    /**
     * Event channel to emit processor-specific events
     */
    val events: SharedFlow<E>
    
    /**
     * Flow of input requests that require user interaction
     */
    val userInputRequests: SharedFlow<UserInputRequest>
    
    /**
     * Process a queue item
     */
    suspend fun process(item: T): ProcessingResult
    
    /**
     * Provide input to a processor that is waiting for input
     * 
     * @param response The response to the input request
     */
    suspend fun provideInput(response: UserInputResponse)
    
    /**
     * Abort the current processing operation
     * Allows processors to implement graceful termination with processor-specific cleanup logic
     * 
     * @param item The item being processed that should be aborted, or null to abort any current operation
     * @return True if the processor was successfully aborted, false otherwise
     */
    suspend fun abort(item: T? = null): Boolean
}
