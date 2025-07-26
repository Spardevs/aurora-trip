package br.com.ticpass.pos.queue.payment.usecases

import android.util.Log
import br.com.ticpass.pos.queue.HybridQueueManager
import br.com.ticpass.pos.queue.InputResponse
import br.com.ticpass.pos.queue.QueueInputResponse
import br.com.ticpass.pos.queue.payment.PaymentQueueInputResponse
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
    fun confirmNextProcessor(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (UiState) -> Unit
    ): SideEffect {
        updateState(UiState.Processing)
        return SideEffect.ProvideQueueInput { 
            paymentQueue.provideQueueInput(QueueInputResponse.proceed(requestId))
        }
    }
    
    /**
     * Confirm proceeding to the next processor with modified payment details
     */
    fun confirmNextProcessorWithModifiedPayment(
        requestId: String,
        modifiedAmount: Int,
        modifiedMethod: SystemPaymentMethod,
        modifiedProcessorType: PaymentProcessorType,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (UiState) -> Unit
    ): SideEffect {
        updateState(UiState.Processing)
        val response = PaymentQueueInputResponse.proceedWithModifiedPayment(
            requestId = requestId,
            modifiedAmount = modifiedAmount,
            modifiedMethod = modifiedMethod,
            modifiedProcessorType = modifiedProcessorType
        )
        return SideEffect.ProvideQueueInput { paymentQueue.provideQueueInput(response) }
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
}
