package br.com.ticpass.pos.domain.refund.usecase

import br.com.ticpass.pos.presentation.refund.states.RefundUiEvent
import br.com.ticpass.pos.core.queue.core.HybridQueueManager
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.presentation.refund.states.RefundSideEffect
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.core.queue.processors.refund.processors.models.RefundProcessorType
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for handling queue management operations
 */
class QueueManagementUseCase @Inject constructor() {
    
    /**
     * Start processing the refund queue
     */
    fun startProcessing(
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>,
        emitUiEvent: (RefundUiEvent) -> Unit
    ): RefundSideEffect {
        emitUiEvent(RefundUiEvent.ShowToast("Starting refund processing"))
        return RefundSideEffect.StartProcessingQueue { refundQueue.startProcessing() }
    }
    
    /**
     * Enqueue a new refund
     */
    fun enqueueRefund(
        atk: String,
        txId: String,
        isQRCode: Boolean,
        processorType: RefundProcessorType,
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>,
        emitUiEvent: (RefundUiEvent) -> Unit
    ): RefundSideEffect {
        val refundItem = RefundQueueItem(
            id = UUID.randomUUID().toString(),
            atk = atk,
            txId = txId,
            isQRCode = isQRCode,
            processorType = processorType,
            priority = 10,
        )
        emitUiEvent(RefundUiEvent.ShowToast("Refund added to queue"))
        return RefundSideEffect.EnqueueRefundItem { refundQueue.enqueue(refundItem) }
    }
    
    /**
     * Cancel a specific refund
     */
    fun cancelRefund(
        refundId: String,
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>,
        emitUiEvent: (RefundUiEvent) -> Unit
    ): RefundSideEffect {
        return RefundSideEffect.RemoveRefundItem {
            val item = refundQueue.queueState.value.find { it.id == refundId }
            if (item != null) {
                refundQueue.remove(item)
                emitUiEvent(RefundUiEvent.ShowToast("Refund cancelled"))
            }
        }
    }
    
    /**
     * Cancel all refunds
     */
    fun clearQueue(
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>,
        emitUiEvent: (RefundUiEvent) -> Unit
    ): RefundSideEffect {
        emitUiEvent(RefundUiEvent.ShowToast("All refunds cancelled"))
        return RefundSideEffect.ClearRefundQueue { refundQueue.clearQueue() }
    }

    fun abortCurrentRefund(
        refundQueue: HybridQueueManager<RefundQueueItem, RefundEvent>,
        emitUiEvent: (RefundUiEvent) -> Unit
    ): RefundSideEffect {
        emitUiEvent(RefundUiEvent.ShowToast("Aborting current refund"))
        return RefundSideEffect.AbortCurrentRefund { refundQueue.abort() }
    }
}
