package br.com.ticpass.pos.queue.processors.nfc.processors.core

import android.util.Log
import br.com.ticpass.pos.nfc.models.NFCTagDetectionResult
import br.com.ticpass.pos.nfc.models.NFCTagSectorKeyType
import br.com.ticpass.pos.queue.core.QueueProcessor
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.nfc.exceptions.NFCException
import br.com.ticpass.pos.queue.processors.nfc.models.NFCEvent
import br.com.ticpass.pos.queue.processors.nfc.models.NFCQueueItem
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCTagDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Base class for nfc processors
 * Contains common event emission functionality and input request handling
 */
abstract class NFCProcessorBase :
    QueueProcessor<NFCQueueItem, NFCEvent> {

    private val TAG  = "NFCProcessor"
    // Shared flow for nfc events
    protected val _events = MutableSharedFlow<NFCEvent>(replay = 0, extraBufferCapacity = 10)
    override val events: SharedFlow<NFCEvent> = _events.asSharedFlow()
    
    // Input request/response flows
    protected val _userInputRequests = MutableSharedFlow<UserInputRequest>(replay = 0, extraBufferCapacity = 3)
    override val userInputRequests: SharedFlow<UserInputRequest> = _userInputRequests.asSharedFlow()
    
    // For receiving input responses
    protected val _userInputResponses = MutableSharedFlow<UserInputResponse>(replay = 0, extraBufferCapacity = 3)

    /**
     * Public processing method - delegates to protected template method
     */
    override suspend fun process(item: NFCQueueItem): ProcessingResult {
        _events.emit(NFCEvent.START)

        // Call implementation-specific processing
        return processNFC(item)
    }

    /**
     * Detects the NFC card by waiting for it to be present.
     * Emits an event to indicate that user should reach the tag.
     * If the detection times out or fails, it throws an NFCException.
     *
     * @param timeout The maximum time to wait for the card detection.
     * @return The detected NFCTagDetectionResult.
     */
    protected suspend fun detectTag(timeout: Long = 5000L): NFCTagDetectionResult {
        return withContext(Dispatchers.IO) {
            try {
                _events.tryEmit(NFCEvent.REACH_TAG(timeout))
                val result = NFCTagDetector.detectTag(timeout)
                    ?: throw NFCException(ProcessingErrorEvent.NFC_TAG_REACH_TIMEOUT)

                return@withContext result
            }
            catch (exception: NFCException) {
                Log.e(TAG, "❌ Error during card detection", exception)
                throw exception
            }
            catch (exception: Exception) {
                Log.e(TAG, "❌ Error during card detection", exception)
                throw NFCException(ProcessingErrorEvent.GENERIC)
            }
        }
    }

    /**
     * Requests NFC keys.
     */
    protected suspend fun requestNFCKeys(): Map<NFCTagSectorKeyType, String> {
        return withContext(Dispatchers.IO) {
            requestUserInput(
                UserInputRequest.CONFIRM_NFC_KEYS()
            )
        }.value as? Map<NFCTagSectorKeyType, String> ?: throw NFCException(ProcessingErrorEvent.NFC_TAG_MISSING_KEYS)
    }
    
    /**
     * Template method that routes to the appropriate typed processor method
     * Handles the actual nfc processing logic based on item type
     */
    protected suspend fun processNFC(item: NFCQueueItem): ProcessingResult {
        return when (item) {
            is NFCQueueItem.CustomerAuthOperation -> process(item)
            is NFCQueueItem.TagFormatOperation -> process(item)
            is NFCQueueItem.CustomerSetupOperation -> process(item)
            is NFCQueueItem.CartReadOperation -> process(item)
            is NFCQueueItem.CartUpdateOperation -> process(item)
        }
    }
    
    /**
     * Overloaded processor methods to be implemented by concrete processors
     * Each processor only needs to implement the overloads for operations it supports
     */
    protected open suspend fun process(item: NFCQueueItem.CustomerAuthOperation): ProcessingResult {
        throw UnsupportedOperationException("Auth operation not supported by ${this::class.simpleName}")
    }

    protected open suspend fun process(item: NFCQueueItem.TagFormatOperation): ProcessingResult {
        throw UnsupportedOperationException("Format operation not supported by ${this::class.simpleName}")
    }
    
    protected open suspend fun process(item: NFCQueueItem.CustomerSetupOperation): ProcessingResult {
        throw UnsupportedOperationException("Setup operation not supported by ${this::class.simpleName}")
    }
    
    protected open suspend fun process(item: NFCQueueItem.CartReadOperation): ProcessingResult {
        throw UnsupportedOperationException("Cart read operation not supported by ${this::class.simpleName}")
    }
    
    protected open suspend fun process(item: NFCQueueItem.CartUpdateOperation): ProcessingResult {
        throw UnsupportedOperationException("Cart update operation not supported by ${this::class.simpleName}")
    }
    
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
    override suspend fun abort(item: NFCQueueItem?): Boolean {
        // Emit a cancellation event
        _events.emit(NFCEvent.CANCELLED)
        
        // Cancel any pending input requests by emitting a cancellation response
        // This will unblock any processor waiting for input
        _userInputRequests.replayCache.forEach { request ->
            _userInputResponses.emit(UserInputResponse.canceled(request.id))
        }

        return onAbort(item)
    }

    /**
     * Abstract method to be implemented by concrete processors
     * Handles the actual nfc aborting logic
     */
    protected abstract suspend fun onAbort(item: NFCQueueItem?): Boolean
    
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
