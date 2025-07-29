package br.com.ticpass.pos.queue.processors.printing.processors.core

import br.com.ticpass.pos.queue.input.InputRequest
import br.com.ticpass.pos.queue.input.InputResponse
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.core.QueueProcessor
import br.com.ticpass.pos.queue.processors.printing.models.PrintQueueItem
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Print Processor
 * Handles the processing of print queue items with event emission
 */
class PrintProcessor : QueueProcessor<PrintQueueItem, PrintingEvent> {
    // Event flow implementation
    private val _events = MutableSharedFlow<PrintingEvent>()
    override val events: SharedFlow<PrintingEvent> = _events.asSharedFlow()
    override val inputRequests: SharedFlow<InputRequest>
        get() = TODO("Not yet implemented")

    override suspend fun provideInput(response: InputResponse) {
        TODO("Not yet implemented")
    }

    override suspend fun abort(item: PrintQueueItem?): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun process(item: PrintQueueItem): ProcessingResult {
        return ProcessingResult.Retry
    }
}
