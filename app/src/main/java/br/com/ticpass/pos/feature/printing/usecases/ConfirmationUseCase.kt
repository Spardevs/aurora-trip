package br.com.ticpass.pos.feature.printing.usecases

import android.util.Log
import br.com.ticpass.pos.feature.printing.state.PrintingUiState
import br.com.ticpass.pos.queue.core.BaseProcessingEvent
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.input.QueueInputResponse
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.feature.printing.state.PrintingSideEffect
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrinterNetworkInfo
import br.com.ticpass.pos.queue.processors.printing.models.PaperCutType
import javax.inject.Inject

/**
 * Use case for handling processor confirmation operations
 */
class ConfirmationUseCase @Inject constructor() {
    
    /**
     * Confirm proceeding to the next processor
     */
    fun <T : QueueItem, E : BaseProcessingEvent> confirmProcessor(
        requestId: String,
        queue: HybridQueueManager<T, E>,
        modifiedItem: T,
        updateState: (PrintingUiState) -> Unit
    ): PrintingSideEffect {
        updateState(PrintingUiState.Processing)
        Log.d("ConfirmationUseCase", "confirmProcessor called with requestId: $requestId, modifiedItem: $modifiedItem")
        return PrintingSideEffect.ProvideQueueInput {
            queue.replaceCurrentItem(modifiedItem)
            queue.provideQueueInput(QueueInputResponse.proceed(requestId))
        }
    }
    
    /**
     * Skip the current processor (for confirmation dialogs)
     */
    fun skipProcessor(
        requestId: String,
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>
    ): PrintingSideEffect {
        return PrintingSideEffect.ProvideQueueInput {
            printingQueue.provideQueueInput(QueueInputResponse.skip(requestId))
        }
    }
    
    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(
        requestId: String,
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>
    ): PrintingSideEffect {
        return PrintingSideEffect.ProvideQueueInput {
            printingQueue.provideQueueInput(QueueInputResponse.onErrorSkip(requestId))
        }
    }

    /**
     * Confirm printer network info
     */
    fun confirmPrinterNetworkInfo(
        requestId: String,
        networkInfo: PrinterNetworkInfo,
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        updateState: (PrintingUiState) -> Unit
    ): PrintingSideEffect {
        updateState(PrintingUiState.Processing)
        // Create an input response with the network info as the value
        val response = UserInputResponse(requestId, networkInfo)
        // Provide input directly to the processor instead of using queue input
        return PrintingSideEffect.ProvideProcessorInput {
            printingQueue.processor.provideUserInput(response)
        }
    }
    
    /**
     * Confirm printer paper cut type
     */
    fun confirmPrinterPaperCut(
        requestId: String,
        paperCutType: PaperCutType,
        printingQueue: HybridQueueManager<PrintingQueueItem, PrintingEvent>,
        updateState: (PrintingUiState) -> Unit
    ): PrintingSideEffect {
        updateState(PrintingUiState.Processing)
        // Create an input response with the paper cut type as the value
        val response = UserInputResponse(requestId, paperCutType)
        // Provide input directly to the processor instead of using queue input
        return PrintingSideEffect.ProvideProcessorInput {
            printingQueue.processor.provideUserInput(response)
        }
    }
}
