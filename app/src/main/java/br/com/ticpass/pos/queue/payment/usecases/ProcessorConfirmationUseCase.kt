package br.com.ticpass.pos.queue.payment.usecases

import android.util.Log
import br.com.ticpass.pos.queue.BaseProcessingEvent
import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.InputResponse
import br.com.ticpass.pos.queue.QueueInputResponse
import br.com.ticpass.pos.queue.QueueItem
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.queue.payment.SystemPaymentMethod
import br.com.ticpass.pos.queue.payment.processors.PaymentProcessorType
import br.com.ticpass.pos.queue.payment.state.SideEffect
import br.com.ticpass.pos.queue.payment.state.UiState
import javax.inject.Inject

/**
 * Use case for handling processor confirmation operations
 */
class ProcessorConfirmationUseCase @Inject constructor() {
    
    /**
     * Confirm proceeding to the next processor
     */
    fun <T : QueueItem, E : BaseProcessingEvent> confirmProcessor(
        requestId: String,
        queue: HybridQueueManager<T, E>,
        modifiedItem: T,
        updateState: (UiState) -> Unit
    ): SideEffect {
        updateState(UiState.Processing)
        Log.d("ProcessorConfirmationUseCase", "confirmProcessor called with requestId: $requestId, modifiedItem: $modifiedItem")
        return SideEffect.ProvideQueueInput {
            queue.replaceCurrentItem(modifiedItem)
            queue.provideQueueInput(QueueInputResponse.proceed(requestId))
        }
    }
    
    /**
     * Confirm proceeding to the next processor with modified payment details
     */
    fun confirmPayment(
        requestId: String,
        amount: Int,
        method: SystemPaymentMethod,
        processorType: PaymentProcessorType,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (UiState) -> Unit
    ): SideEffect {
        updateState(UiState.Processing)
        
        // Handle item modification at the UseCase layer
        return SideEffect.ProvideQueueInput {
            // First, modify the current item in the queue
            val currentItem = paymentQueue.getCurrentItem() as? ProcessingPaymentQueueItem
            if (currentItem != null) {
                val modifiedItem = currentItem.copy(
                    amount = amount,
                    method = method,
                    processorType = processorType
                )
                paymentQueue.replaceCurrentItem(modifiedItem)
            }
            
            // Then provide the proceed response
            paymentQueue.provideQueueInput(QueueInputResponse.proceed(requestId))
        }
    }
    
    /**
     * Skip the current processor
     */
    fun skipProcessor(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>
    ): SideEffect {
        return SideEffect.ProvideQueueInput { 
            paymentQueue.provideQueueInput(QueueInputResponse.skip(requestId)) 
        }
    }
    
    /**
     * Confirm customer receipt printing
     */
    fun confirmCustomerReceiptPrinting(
        requestId: String,
        shouldPrint: Boolean,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (UiState) -> Unit
    ): SideEffect {
        updateState(UiState.Processing)
        // Create an input response with the print choice as the value
        val response = InputResponse(requestId, shouldPrint)
        // Provide input directly to the processor instead of using queue input
        return SideEffect.ProvideProcessorInput { paymentQueue.processor.provideInput(response) }
    }
    
    /**
     * Confirm merchant PIX key
     */
    fun confirmMerchantPixKey(
        requestId: String,
        pixKey: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (UiState) -> Unit
    ): SideEffect {
        updateState(UiState.Processing)
        // Create an input response with the PIX key as the value
        val response = InputResponse(requestId, pixKey)
        // Provide input directly to the processor instead of using queue input
        return SideEffect.ProvideProcessorInput { 
            paymentQueue.processor.provideInput(response) 
        }
    }
    
    /**
     * Confirm merchant PIX has been paid
     */
    fun confirmMerchantPixHasBeenPaid(
        requestId: String,
        didPay: Boolean,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (UiState) -> Unit
    ): SideEffect {
        updateState(UiState.Processing)
        // Create an input response with the payment confirmation as the value
        val response = InputResponse(requestId, didPay)
        // Provide input directly to the processor instead of using queue input
        return SideEffect.ProvideProcessorInput { 
            paymentQueue.processor.provideInput(response) 
        }
    }
}
