package br.com.ticpass.pos.feature.payment.usecases

import android.util.Log
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiState
import br.com.ticpass.pos.queue.core.BaseProcessingEvent
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.input.UserInputResponse
import br.com.ticpass.pos.queue.input.QueueInputResponse
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingSideEffect
import br.com.ticpass.pos.queue.processors.payment.models.ProcessingPaymentQueueItem
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
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        updateState(PaymentProcessingUiState.Processing)
        Log.d("ConfirmationUseCase", "confirmProcessor called with requestId: $requestId, modifiedItem: $modifiedItem")
        return PaymentProcessingSideEffect.ProvideQueueInput {
            queue.replaceCurrentItem(modifiedItem)
            queue.provideQueueInput(QueueInputResponse.proceed(requestId))
        }
    }
    
    /**
     * Skip the current processor (for confirmation dialogs)
     */
    fun skipProcessor(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>
    ): PaymentProcessingSideEffect {
        return PaymentProcessingSideEffect.ProvideQueueInput {
            paymentQueue.provideQueueInput(QueueInputResponse.skip(requestId))
        }
    }
    
    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(
        requestId: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>
    ): PaymentProcessingSideEffect {
        return PaymentProcessingSideEffect.ProvideQueueInput {
            paymentQueue.provideQueueInput(QueueInputResponse.onErrorSkip(requestId))
        }
    }
    
    /**
     * Confirm customer receipt printing
     */
    fun confirmCustomerReceiptPrinting(
        requestId: String,
        shouldPrint: Boolean,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        updateState(PaymentProcessingUiState.Processing)
        // Create an input response with the print choice as the value
        val response = UserInputResponse(requestId, shouldPrint)
        // Provide input directly to the processor instead of using queue input
        return PaymentProcessingSideEffect.ProvideProcessorInput { paymentQueue.processor.provideUserInput(response) }
    }
    
    /**
     * Confirm merchant PIX key
     */
    fun confirmMerchantPixKey(
        requestId: String,
        pixKey: String,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        updateState(PaymentProcessingUiState.Processing)
        // Create an input response with the PIX key as the value
        val response = UserInputResponse(requestId, pixKey)
        // Provide input directly to the processor instead of using queue input
        return PaymentProcessingSideEffect.ProvideProcessorInput {
            paymentQueue.processor.provideUserInput(response)
        }
    }
    
    /**
     * Confirm merchant PIX has been paid
     */
    fun confirmMerchantPixHasBeenPaid(
        requestId: String,
        didPay: Boolean,
        paymentQueue: HybridQueueManager<ProcessingPaymentQueueItem, ProcessingPaymentEvent>,
        updateState: (PaymentProcessingUiState) -> Unit
    ): PaymentProcessingSideEffect {
        updateState(PaymentProcessingUiState.Processing)
        // Create an input response with the payment confirmation as the value
        val response = UserInputResponse(requestId, didPay)
        // Provide input directly to the processor instead of using queue input
        return PaymentProcessingSideEffect.ProvideProcessorInput {
            paymentQueue.processor.provideUserInput(response)
        }
    }
}
