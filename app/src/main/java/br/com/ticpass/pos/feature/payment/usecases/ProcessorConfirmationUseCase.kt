package br.com.ticpass.pos.feature.payment.usecases

import android.util.Log
import br.com.ticpass.pos.feature.payment.state.UiState
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.BaseProcessingEvent
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.input.QueueInputResponse
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.queue.processors.payment.processors.models.PaymentProcessorType
import br.com.ticpass.pos.feature.payment.state.SideEffect
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
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
        val response = UserInputResponse(requestId, shouldPrint)
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
        val response = UserInputResponse(requestId, pixKey)
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
        val response = UserInputResponse(requestId, didPay)
        // Provide input directly to the processor instead of using queue input
        return SideEffect.ProvideProcessorInput {
            paymentQueue.processor.provideInput(response) 
        }
    }
}
