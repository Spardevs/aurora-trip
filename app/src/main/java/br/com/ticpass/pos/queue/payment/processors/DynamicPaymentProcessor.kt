package br.com.ticpass.pos.queue.payment.processors

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
        val processor = processorMap[item.processorType.lowercase()] ?: 
                        processorMap["acquirer"] ?: // Fallback to acquirer
                        return ProcessingResult.Error("No suitable processor found for type: ${item.processorType}")
        
        // Forward the start event from the base processor
        _events.emit(ProcessingPaymentEvent.Started(item.id, item.amount))
        
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
    
    /**
     * Launch a coroutine that forwards events from the delegate processor to this processor's event flow
     * This prevents event duplication (Started event)
     */
    private fun launchEventForwarding(sourceEvents: SharedFlow<ProcessingPaymentEvent>): Job {
        // Use a safe scope rather than GlobalScope
        val scope = CoroutineScope(Dispatchers.Default)
        return scope.launch {
            sourceEvents.collect { event ->
                // Avoid duplicating the Started event that we already emitted
                if (event !is ProcessingPaymentEvent.Started) {
                    _events.emit(event)
                }
            }
        }
    }
}
