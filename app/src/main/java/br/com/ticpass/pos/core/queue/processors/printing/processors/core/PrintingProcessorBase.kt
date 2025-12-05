package br.com.ticpass.pos.core.queue.processors.printing.processors.core

import android.content.ContentValues.TAG
import android.util.Log
import br.com.ticpass.pos.core.queue.core.QueueProcessor
import br.com.ticpass.pos.core.queue.input.UserInputRequest
import br.com.ticpass.pos.core.queue.input.UserInputResponse
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingQueueItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Base class for printing processors
 * Contains common event emission functionality and input request handling
 */
abstract class PrintingProcessorBase :
    QueueProcessor<PrintingQueueItem, PrintingEvent> {
    
    // Shared flow for printing events
    protected val _events = MutableSharedFlow<PrintingEvent>(replay = 0, extraBufferCapacity = 10)
    override val events: SharedFlow<PrintingEvent> = _events.asSharedFlow()
    
    // Input request/response flows
    protected val _userInputRequests = MutableSharedFlow<UserInputRequest>(replay = 0, extraBufferCapacity = 3)
    override val userInputRequests: SharedFlow<UserInputRequest> = _userInputRequests.asSharedFlow()
    
    // For receiving input responses
    protected val _userInputResponses = MutableSharedFlow<UserInputResponse>(replay = 0, extraBufferCapacity = 3)

    /**
     * Public processing method - delegates to protected template method
     */
    override suspend fun process(item: PrintingQueueItem): ProcessingResult {
        _events.emit(PrintingEvent.START)

        Log.d(TAG, "Processing print job for item: ${item.id}")

        // Call implementation-specific processing
        return processPrinting(item)
    }
    
    /**
     * Abstract method to be implemented by concrete processors
     * Handles the actual printing processing logic
     */
    protected abstract suspend fun processPrinting(item: PrintingQueueItem): ProcessingResult
    
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
    override suspend fun abort(item: PrintingQueueItem?): Boolean {
        // Emit a cancellation event
        _events.emit(PrintingEvent.CANCELLED)
        
        // Cancel any pending input requests by emitting a cancellation response
        // This will unblock any processor waiting for input
        _userInputRequests.replayCache.forEach { request ->
            _userInputResponses.emit(UserInputResponse.canceled(request.id))
        }

        return onAbort(item)
    }

    /**
     * Abstract method to be implemented by concrete processors
     * Handles the actual printing aborting logic
     */
    protected abstract suspend fun onAbort(item: PrintingQueueItem?): Boolean
    
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
