package br.com.ticpass.pos.feature.printing.usecases

import br.com.ticpass.pos.feature.printing.state.PrintingUiEvent
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.feature.printing.state.PrintingSideEffect
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for handling queue management operations
 */
class QueueManagementUseCase @Inject constructor() {
    
    /**
     * Start processing the printing queue
     */
    fun startProcessing(
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        emitUiEvent: (PrintingUiEvent) -> Unit
    ): PrintingSideEffect {
        emitUiEvent(PrintingUiEvent.ShowToast("Starting printing processing"))
        return PrintingSideEffect.StartProcessingQueue { printingQueue.startProcessing() }
    }
    
    /**
     * Enqueue a new printing
     */
    fun enqueuePrinting(
        filePath: String,
        processorType: PrintingProcessorType,
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        emitUiEvent: (PrintingUiEvent) -> Unit
    ): PrintingSideEffect {
        val printingItem = PrintingQueueItem(
            id = UUID.randomUUID().toString(),
            filePath = filePath,
            processorType = processorType,
            priority = 10,
        )
        emitUiEvent(PrintingUiEvent.ShowToast("Printing added to queue"))
        return PrintingSideEffect.EnqueuePrintingItem { printingQueue.enqueue(printingItem) }
    }
    
    /**
     * Cancel a specific printing
     */
    fun cancelPrinting(
        printingId: String,
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        emitUiEvent: (PrintingUiEvent) -> Unit
    ): PrintingSideEffect {
        return PrintingSideEffect.RemovePrintingItem {
            val item = printingQueue.queueState.value.find { it.id == printingId }
            if (item != null) {
                printingQueue.remove(item)
                emitUiEvent(PrintingUiEvent.ShowToast("Printing cancelled"))
            }
        }
    }
    
    /**
     * Cancel all printings
     */
    fun clearQueue(
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        emitUiEvent: (PrintingUiEvent) -> Unit
    ): PrintingSideEffect {
        emitUiEvent(PrintingUiEvent.ShowToast("All printings cancelled"))
        return PrintingSideEffect.ClearPrintingQueue { printingQueue.clearQueue() }
    }

    fun abortCurrentPrinting(
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        emitUiEvent: (PrintingUiEvent) -> Unit
    ): PrintingSideEffect {
        emitUiEvent(PrintingUiEvent.ShowToast("Aborting current printing"))
        return PrintingSideEffect.AbortCurrentPrinting { printingQueue.abort() }
    }
}
