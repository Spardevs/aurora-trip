package br.com.ticpass.pos.queue.payment.processors

import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.InputResponse
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.QueueProcessor
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Base class for payment processors
 * Contains common event emission functionality and input request handling
 */
abstract class PaymentProcessorBase : QueueProcessor<ProcessingPaymentQueueItem, ProcessingPaymentEvent> {
    
    // Shared flow for payment events
    protected val _events = MutableSharedFlow<ProcessingPaymentEvent>(replay = 0, extraBufferCapacity = 10)
    override val events: SharedFlow<ProcessingPaymentEvent> = _events.asSharedFlow()
    
    // Input request/response flows
    protected val _inputRequests = MutableSharedFlow<InputRequest>(replay = 0, extraBufferCapacity = 3)
    override val inputRequests: SharedFlow<InputRequest> = _inputRequests.asSharedFlow()
    
    // For receiving input responses
    private val _inputResponses = MutableSharedFlow<InputResponse>(replay = 0, extraBufferCapacity = 3)

    /**
     * Public processing method - delegates to protected template method
     */
    override suspend fun process(item: ProcessingPaymentQueueItem): ProcessingResult {
        _events.emit(ProcessingPaymentEvent.START)

        // Call implementation-specific processing
        return processPayment(item)
    }
    
    /**
     * Abstract method to be implemented by concrete processors
     * Handles the actual payment processing logic
     */
    protected abstract suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult
    
    /**
     * Provide input to the processor (from UI)
     * Implementation of QueueProcessor.provideInput
     */
    override suspend fun provideInput(response: InputResponse) {
        _inputResponses.emit(response)
    }
    
    /**
     * Request input from the user and wait for response
     * 
     * @param request The input request
     * @return The response or null if timed out or canceled
     */
    protected suspend fun requestInput(request: InputRequest): InputResponse {
        // Emit request to UI
        _inputRequests.emit(request)
        
        // Wait for response with optional timeout
        return withTimeoutOrNull(request.timeoutMs ?: Long.MAX_VALUE) {
            _inputResponses.first { it.requestId == request.id }
        } ?: InputResponse.timeout(request.id)
    }
}
