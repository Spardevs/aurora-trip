package br.com.ticpass.pos.feature.refund.usecases

import android.util.Log
import br.com.ticpass.pos.feature.refund.state.RefundUiState
import br.com.ticpass.pos.queue.core.BaseProcessingEvent
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.input.QueueInputResponse
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.feature.refund.state.RefundSideEffect
import br.com.ticpass.pos.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.queue.processors.refund.processors.models.PrinterNetworkInfo
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
        updateState: (RefundUiState) -> Unit
    ): RefundSideEffect {
        updateState(RefundUiState.Processing)
        Log.d("ConfirmationUseCase", "confirmProcessor called with requestId: $requestId, modifiedItem: $modifiedItem")
        return RefundSideEffect.ProvideQueueInput {
            queue.replaceCurrentItem(modifiedItem)
            queue.provideQueueInput(QueueInputResponse.proceed(requestId))
        }
    }
    
    /**
     * Skip the current processor (for confirmation dialogs)
     */
    fun skipProcessor(
        requestId: String,
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>
    ): RefundSideEffect {
        return RefundSideEffect.ProvideQueueInput {
            refundQueue.provideQueueInput(QueueInputResponse.skip(requestId))
        }
    }
    
    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(
        requestId: String,
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>
    ): RefundSideEffect {
        return RefundSideEffect.ProvideQueueInput {
            refundQueue.provideQueueInput(QueueInputResponse.onErrorSkip(requestId))
        }
    }

    /**
     * Confirm printer network info
     */
    fun confirmPrinterNetworkInfo(
        requestId: String,
        networkInfo: PrinterNetworkInfo,
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>,
        updateState: (RefundUiState) -> Unit
    ): RefundSideEffect {
        updateState(RefundUiState.Processing)
        // Create an input response with the network info as the value
        val response = UserInputResponse(requestId, networkInfo)
        // Provide input directly to the processor instead of using queue input
        return RefundSideEffect.ProvideProcessorInput {
            refundQueue.processor.provideUserInput(response)
        }
    }
}
