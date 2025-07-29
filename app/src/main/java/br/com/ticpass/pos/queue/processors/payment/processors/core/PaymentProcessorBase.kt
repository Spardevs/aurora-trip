package br.com.ticpass.pos.queue.processors.payment.processors.core

import android.util.Log
import br.com.ticpass.pos.queue.core.QueueProcessor
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Base class for payment processors
 * Contains common event emission functionality and input request handling
 */
abstract class PaymentProcessorBase :
    QueueProcessor<ProcessingPaymentQueueItem, ProcessingPaymentEvent> {
    
    // Shared flow for payment events
    protected val _events = MutableSharedFlow<ProcessingPaymentEvent>(replay = 0, extraBufferCapacity = 10)
    override val events: SharedFlow<ProcessingPaymentEvent> = _events.asSharedFlow()
    
    // Input request/response flows
    protected val _userInputRequests = MutableSharedFlow<UserInputRequest>(replay = 0, extraBufferCapacity = 3)
    override val userInputRequests: SharedFlow<UserInputRequest> = _userInputRequests.asSharedFlow()
    
    // For receiving input responses
    protected val _userInputResponses = MutableSharedFlow<UserInputResponse>(replay = 0, extraBufferCapacity = 3)

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
    override suspend fun provideInput(response: UserInputResponse) {
        _userInputResponses.emit(response)
    }
    
    /**
     * Abort the current processing operation
     * Base implementation that can be overridden by concrete processors for specific abort logic
     * 
     * @param item The item being processed that should be aborted, or null to abort any current operation
     * @return True if the processor was successfully aborted, false otherwise
     */
    override suspend fun abort(item: ProcessingPaymentQueueItem?): Boolean {
        // Emit a cancellation event
        _events.emit(ProcessingPaymentEvent.CANCELLED)
        
        // Cancel any pending input requests by emitting a cancellation response
        // This will unblock any processor waiting for input
        _userInputRequests.replayCache.forEach { request ->
            _userInputResponses.emit(UserInputResponse.canceled(request.id))
        }
        Log.d("PaymentProcessorBase", "Aborting payment for item: ${item?.id ?: "unknown"}")
        
        return onAbort(item)
    }

    /**
     * Abstract method to be implemented by concrete processors
     * Handles the actual payment aborting logic
     */
    protected abstract suspend fun onAbort(item: ProcessingPaymentQueueItem?): Boolean
    
    /**
     * Request input from the user and wait for response
     * 
     * @param request The input request
     * @return The response or null if timed out or canceled
     */
    protected suspend fun requestUserInput(request: UserInputRequest): UserInputResponse {
        // Emit request to UI
        _userInputRequests.emit(request)
        
        // Wait for response with optional timeout
        return withTimeoutOrNull(request.timeoutMs ?: Long.MAX_VALUE) {
            _userInputResponses.first { it.requestId == request.id }
        } ?: UserInputResponse.timeout(request.id)
    }
}
