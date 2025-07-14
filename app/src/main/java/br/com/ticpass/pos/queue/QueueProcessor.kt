package br.com.ticpass.pos.queue

import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
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
    val inputRequests: SharedFlow<InputRequest>
    
    /**
     * Process a queue item
     */
    suspend fun process(item: T): ProcessingResult
    
    /**
     * Provide input to a processor that is waiting for input
     * 
     * @param response The response to the input request
     */
    suspend fun provideInput(response: InputResponse)
}
