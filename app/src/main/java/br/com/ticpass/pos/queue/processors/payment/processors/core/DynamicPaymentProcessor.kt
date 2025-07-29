package br.com.ticpass.pos.queue.processors.payment.processors.core

import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType

/**
 * Dynamic Payment Processor
 * A processor that delegates to different concrete processors based on the payment item's processor type
 * Allows multiple payment types to be processed in a single queue
 */
class DynamicPaymentProcessor(
    private val processorMap: Map<PaymentProcessorType, PaymentProcessorBase>
) : PaymentProcessorBase() {
    
    // Keep track of the current delegate processor to forward input responses
    private var currentDelegateProcessor: PaymentProcessorBase? = null
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        // Get the processor based on the item's processor type
        val processor = processorMap[item.processorType] ?:
                        processorMap[PaymentProcessorType.ACQUIRER] ?: // Fallback to acquirer
                        return ProcessingResult.Error(ProcessingErrorEvent.PROCESSOR_NOT_FOUND)
        
        // Store the current delegate processor to forward input responses
        currentDelegateProcessor = processor
        
        // Forward the start event from the base processor
        _events.emit(ProcessingPaymentEvent.START)
        
        // Collect events and input requests from the delegate processor and re-emit them
        val processorEvents = processor.events
        val processorInputRequests = processor.userInputRequests
        
        // Launch jobs to forward both events and input requests
        val eventJob = launchEventForwarding(processorEvents)
        val inputRequestJob = launchInputRequestForwarding(processorInputRequests)
        
        try {
            // We can't directly call processPayment since it's protected
            // Instead, we'll use process() which is the public API
            return processor.process(item)
        } finally {
            // Cancel event and input request forwarding when done
            eventJob.cancel()
            inputRequestJob.cancel()
            // Clear the current delegate processor reference
            currentDelegateProcessor = null
        }
    }

    override suspend fun onAbort(item: ProcessingPaymentQueueItem?): Boolean {
        // Get the processor based on the item's processor type
        val processor = processorMap[item?.processorType] ?:
                        processorMap[PaymentProcessorType.ACQUIRER] ?: // Fallback to acquirer
                        return false

        // Forward the start event from the base processor
        _events.emit(ProcessingPaymentEvent.CANCELLED)

        // Collect events and input requests from the delegate processor and re-emit them
        val processorEvents = processor.events
        val processorInputRequests = processor.userInputRequests
        
        // Launch jobs to forward both events and input requests
        val eventJob = launchEventForwarding(processorEvents)
        val inputRequestJob = launchInputRequestForwarding(processorInputRequests)

        try {
            return processor.abort(item)
        } catch (e: Exception) {
            eventJob.cancel()
            inputRequestJob.cancel()
            return false
        }
    }

    /**
     * Launch a coroutine that forwards events from the delegate processor to this processor's event flow
     * This prevents event duplication (START event)
     */
    private fun launchEventForwarding(sourceEvents: SharedFlow<ProcessingPaymentEvent>): Job {
        // Use a safe scope rather than GlobalScope
        val scope = CoroutineScope(Dispatchers.Default)
        return scope.launch {
            sourceEvents.collect { event ->
                // Avoid duplicating the START event that we already emitted
                if (event !is ProcessingPaymentEvent.START) {
                    _events.emit(event)
                }
            }
        }
    }
    
    /**
     * Launch a coroutine that forwards input requests from the delegate processor to this processor's input requests flow
     */
    private fun launchInputRequestForwarding(sourceUserInputRequests: SharedFlow<UserInputRequest>): Job {
        // Use a safe scope rather than GlobalScope
        val scope = CoroutineScope(Dispatchers.Default)
        return scope.launch {
            sourceUserInputRequests.collect { inputRequest ->
                // Forward all input requests
                _userInputRequests.emit(inputRequest)
            }
        }
    }
    
    /**
     * Override provideInput to forward input responses to the delegate processor
     */
    override suspend fun provideUserInput(response: UserInputResponse) {
        // Forward the input response to the current delegate processor if available
        currentDelegateProcessor?.provideUserInput(response)
        // Also emit to our own input responses flow
        _userInputResponses.emit(response)
    }
}
