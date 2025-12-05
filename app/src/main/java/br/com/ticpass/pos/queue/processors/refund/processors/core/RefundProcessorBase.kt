package br.com.ticpass.pos.queue.processors.refund.processors.core

import br.com.ticpass.pos.queue.core.QueueProcessor
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.queue.processors.refund.models.RefundQueueItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Base class for refund processors
 * Contains common event emission functionality and input request handling
 */
abstract class RefundProcessorBase :
    QueueProcessor<RefundQueueItem, RefundEvent> {
    
    // Shared flow for refund events
    protected val _events = MutableSharedFlow<RefundEvent>(replay = 0, extraBufferCapacity = 10)
    override val events: SharedFlow<RefundEvent> = _events.asSharedFlow()
    
    // Input request/response flows
    protected val _userInputRequests = MutableSharedFlow<UserInputRequest>(replay = 0, extraBufferCapacity = 3)
    override val userInputRequests: SharedFlow<UserInputRequest> = _userInputRequests.asSharedFlow()
    
    // For receiving input responses
    protected val _userInputResponses = MutableSharedFlow<UserInputResponse>(replay = 0, extraBufferCapacity = 3)

    /**
     * Public processing method - delegates to protected template method
     */
    override suspend fun process(item: RefundQueueItem): ProcessingResult {
        _events.emit(RefundEvent.START)

        // Call implementation-specific processing
        return processRefund(item)
    }
    
    /**
     * Abstract method to be implemented by concrete processors
     * Handles the actual refund processing logic
     */
    protected abstract suspend fun processRefund(item: RefundQueueItem): ProcessingResult
    
    /**
     * Provide input to the processor (from UI)
     * Implementation of QueueProcessor.provideInput
     */
    override suspend fun provideUserInput(response: UserInputResponse) {
        _userInputResponses.emit(response)
    }
    
    /**
     * Abort the current processing operation
     * Base implementation that can be overridden by concrete processors for specific abort logic
     * 
     * @param item The item being processed that should be aborted, or null to abort any current operation
     * @return True if the processor was successfully aborted, false otherwise
     */
    override suspend fun abort(item: RefundQueueItem?): Boolean {
        // Emit a cancellation event
        _events.emit(RefundEvent.CANCELLED)
        
        // Cancel any pending input requests by emitting a cancellation response
        // This will unblock any processor waiting for input
        _userInputRequests.replayCache.forEach { request ->
            _userInputResponses.emit(UserInputResponse.canceled(request.id))
        }

        return onAbort(item)
    }

    /**
     * Abstract method to be implemented by concrete processors
     * Handles the actual refund aborting logic
     */
    protected abstract suspend fun onAbort(item: RefundQueueItem?): Boolean
    
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
