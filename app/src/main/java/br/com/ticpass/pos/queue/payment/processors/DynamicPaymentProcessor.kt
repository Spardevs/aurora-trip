package br.com.ticpass.pos.queue.payment.processors

import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Dynamic Payment Processor
 * A processor that delegates to different concrete processors based on the payment item's processor type
 * Allows multiple payment types to be processed in a single queue
 */
class DynamicPaymentProcessor(
    private val processorMap: Map<String, PaymentProcessorBase>
) : PaymentProcessorBase() {
    
    /**
     * Constructor that takes a list of processors and builds the map using their class names
     */
    constructor(vararg processors: PaymentProcessorBase) : this(
        processors.associateBy { 
            val className = it::class.java.simpleName.lowercase()
            when {
                className.contains("acquirer") -> "acquirer"
                className.contains("cash") -> "cash"
                className.contains("transaction") -> "transactionless"
                else -> className
            }
        }
    )
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        // Get the processor based on the item's processor type
        val processor = processorMap[item.processorType.name.lowercase()] ?:
                        processorMap["acquirer"] ?: // Fallback to acquirer
                        return ProcessingResult.Error(ProcessingErrorEvent.PROCESSOR_NOT_FOUND)
        
        // Forward the start event from the base processor
        _events.emit(ProcessingPaymentEvent.START)
        
        // Collect events from the delegate processor and re-emit them
        val processorEvents = processor.events
        val job = launchEventForwarding(processorEvents)
        
        try {
            // We can't directly call processPayment since it's protected
            // Instead, we'll use process() which is the public API
            return processor.process(item)
        } finally {
            // Cancel event forwarding when done
            job.cancel()
        }
    }

    override suspend fun onAbort(item: ProcessingPaymentQueueItem?): Boolean {
        // Get the processor based on the item's processor type
        val processor = processorMap[item?.processorType?.name?.lowercase()] ?:
                        processorMap["acquirer"] ?: // Fallback to acquirer
                        return false

        // Forward the start event from the base processor
        _events.emit(ProcessingPaymentEvent.CANCELLED)

        // Collect events from the delegate processor and re-emit them
        val processorEvents = processor.events
        val job = launchEventForwarding(processorEvents)

        try {
            return processor.abort(item)
        } catch (e: Exception) {
            job.cancel()
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
}
