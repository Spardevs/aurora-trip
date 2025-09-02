package br.com.ticpass.pos.feature.payment

import android.os.Handler
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import br.com.ticpass.pos.queue.error.ErrorHandlingAction
import br.com.ticpass.pos.queue.config.ProcessorStartMode
import br.com.ticpass.pos.queue.config.PersistenceStrategy
import br.com.ticpass.pos.queue.core.HybridQueueManager
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingAction
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingReducer
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingUiState
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.queue.core.QueueItem
import br.com.ticpass.pos.queue.processors.payment.utils.PaymentProcessingQueueFactory
import br.com.ticpass.pos.queue.processors.payment.data.PaymentProcessingStorage
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.feature.payment.state.PaymentProcessingSideEffect
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.sdk.payment.PaymentProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch



sealed class PaymentState {
    object Idle : PaymentState()
    object Initializing : PaymentState()
    object Processing : PaymentState()
    data class Success(val transactionId: String) : PaymentState()
    data class Error(val errorMessage: String) : PaymentState()
    object Cancelled : PaymentState()
}

sealed class PaymentEvent {
    data class CardDetected(val cardType: String) : PaymentEvent()
    data class Processing(val message: String) : PaymentEvent()
}

@HiltViewModel
class PaymentProcessingViewModel @Inject constructor(
    private val paymentQueueFactory: PaymentProcessingQueueFactory,
    private val processingPaymentStorage: PaymentProcessingStorage,
    private val reducer: PaymentProcessingReducer
) : ViewModel() {

    inner class ProcessorEventReceived(val event: PaymentProcessingEvent) : PaymentProcessingAction()

    private val paymentTimeoutHandler = Handler()
    private val PAYMENT_TIMEOUT_MS = 30000L

    private val paymentTimeoutRunnable = Runnable {
        _paymentState.value = PaymentState.Error("Tempo limite excedido para o pagamento")
        abortAllPayments()
    }

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Initializing)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _paymentEvents = MutableSharedFlow<PaymentEvent>()
    val paymentEvents: SharedFlow<PaymentEvent> = _paymentEvents.asSharedFlow()

    private lateinit var paymentQueue: HybridQueueManager<PaymentProcessingQueueItem, PaymentProcessingEvent>

    init {
        reducer.initialize(
            emitUiEvent = ::emitUiEvent,
            updateState = ::updateState
        )
        initializePaymentQueue()
    }

    override fun onCleared() {
        super.onCleared()
        clearTimeout()
    }

    private fun launchInViewModelScope(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                updateState(PaymentProcessingUiState.Error(ProcessingErrorEvent.GENERIC))
            }
        }
    }

    val queueState get() = if (::paymentQueue.isInitialized) paymentQueue.queueState else MutableStateFlow(null)
    val fullSize get() = if (::paymentQueue.isInitialized) paymentQueue.fullSize else MutableStateFlow(0)
    val enqueuedSize get() = if (::paymentQueue.isInitialized) paymentQueue.enqueuedSize else MutableStateFlow(0)
    val currentIndex get() = if (::paymentQueue.isInitialized) paymentQueue.currentIndex else MutableStateFlow(0)
    val processingState get() = if (::paymentQueue.isInitialized) paymentQueue.processingState else MutableStateFlow(false)
    val processingPaymentEvents: SharedFlow<PaymentProcessingEvent> get() =
        if (::paymentQueue.isInitialized) paymentQueue.processorEvents else MutableSharedFlow<PaymentProcessingEvent>().asSharedFlow()

    private val _uiEvents = MutableSharedFlow<PaymentProcessingUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    /**
     * Emit a one-time UI event
     */
    private fun emitUiEvent(event: PaymentProcessingUiEvent) {
        launchInViewModelScope {
            _uiEvents.emit(event)
        }
    }

    private val _uiState =
        MutableStateFlow<PaymentProcessingUiState>(PaymentProcessingUiState.Idle)
    val uiState: StateFlow<PaymentProcessingUiState> = _uiState.asStateFlow()

    /**
     * Update the UI state
     */
    private fun updateState(newState: PaymentProcessingUiState) {
        _uiState.value = newState
    }

    /**
     * Initialize the payment queue
     */
    private fun initializePaymentQueue() {
        viewModelScope.launch {
            try {
                paymentQueue = paymentQueueFactory.createDynamicPaymentQueue(
                    storage = processingPaymentStorage,
                    persistenceStrategy = PersistenceStrategy.IMMEDIATE,
                    startMode = ProcessorStartMode.IMMEDIATE,
                    scope = viewModelScope
                )

                // Iniciar coletores de eventos após a inicialização
                startEventCollectors()

                _paymentState.value = PaymentState.Idle
                Log.d("PaymentProcessingViewModel", "Payment queue initialized successfully")

            } catch (e: Exception) {
                Log.e("PaymentProcessingViewModel", "Failed to initialize payment queue", e)
                _paymentState.value = PaymentState.Error("Falha ao inicializar sistema de pagamento")
                updateState(PaymentProcessingUiState.Error(ProcessingErrorEvent.GENERIC))
            }
        }
    }

    /**
     * Start collecting events from the payment queue
     */
    private fun startEventCollectors() {
        // Processor events collector
        viewModelScope.launch {
            paymentQueue.processorEvents.collect { event ->
                Log.d("PaymentProcessingViewModel", "Processor event: ${event.javaClass.simpleName}")

                when (event) {
                    is PaymentProcessingEvent.APPROVAL_SUCCEEDED -> {
                        clearTimeout()
                        val transactionId = event.transactionId
                        _paymentState.value = PaymentState.Success(transactionId as String)
                    }
                    is PaymentProcessingEvent.APPROVAL_DECLINED -> {
                        clearTimeout()
                        _paymentState.value = PaymentState.Error("Pagamento recusado")
                    }
                    is PaymentProcessingEvent.CANCELLED -> {
                        clearTimeout()
                        _paymentState.value = PaymentState.Cancelled
                    }
                    else -> {
                        Log.d("PaymentProcessingViewModel", "Evento não tratado: ${event.javaClass.simpleName}")
                    }
                }

                dispatch(ProcessorEventReceived(event))
            }
        }

        // Processor input requests collector
        viewModelScope.launch {
            paymentQueue.processor.userInputRequests.collect { request ->
                Log.d(
                    "PaymentProcessingViewModel",
                    "Processor input request received: ${request::class.simpleName}"
                )
                dispatch(PaymentProcessingAction.ProcessorInputRequested(request))
            }
        }

        // Processing state collector
        viewModelScope.launch {
            paymentQueue.processingState.collectLatest { state ->
                dispatch(PaymentProcessingAction.ProcessingStateChanged(state))
            }
        }

        // Queue input requests collector
        viewModelScope.launch {
            paymentQueue.queueInputRequests.collectLatest { request ->
                dispatch(PaymentProcessingAction.QueueInputRequested(request))
            }
        }
    }

    /**
     * Dispatch an action to the ViewModel
     * This triggers state transitions and side effects
     */
    private fun dispatch(action: PaymentProcessingAction) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot dispatch action: $action")
            return
        }

        val sideEffect = reducer.reduce(action, paymentQueue)
        sideEffect?.let { executeSideEffect(it) }
    }

    /**
     * Execute a side effect
     * All side effects are executed in the ViewModel scope
     */
    private fun executeSideEffect(sideEffect: PaymentProcessingSideEffect) {
        launchInViewModelScope {
            sideEffect.scope()
        }
    }

    /**
     * Start processing the payment queue
     */
    fun startProcessing() {
        if (!::paymentQueue.isInitialized) {
            Log.e("PaymentProcessingViewModel", "Payment queue not initialized")
            _paymentState.value = PaymentState.Error("Sistema de pagamento não inicializado")
            return
        }

        _paymentState.value = PaymentState.Processing
        dispatch(PaymentProcessingAction.StartProcessing)

        // Iniciar timeout
        paymentTimeoutHandler.removeCallbacks(paymentTimeoutRunnable)
        paymentTimeoutHandler.postDelayed(paymentTimeoutRunnable, PAYMENT_TIMEOUT_MS)
    }

    private fun clearTimeout() {
        paymentTimeoutHandler.removeCallbacks(paymentTimeoutRunnable)
    }

    /**
     * Process a payment with the specified processor type
     * Uses the processor type from the mapper or the provided override
     */
    fun enqueuePayment(
        amount: Int,
        commission: Int,
        method: SystemPaymentMethod,
        isTransactionless: Boolean
    ) {
        if (!::paymentQueue.isInitialized) {
            Log.e("PaymentProcessingViewModel", "Payment queue not initialized")
            _paymentState.value = PaymentState.Error("Sistema de pagamento não inicializado")
            return
        }

        dispatch(
            PaymentProcessingAction.EnqueuePayment(
                amount,
                commission,
                method,
                isTransactionless
            )
        )
    }

    /**
     * Cancel a payment
     */
    fun cancelPayment(paymentId: String) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot cancel payment")
            return
        }

        dispatch(PaymentProcessingAction.CancelPayment(paymentId))
    }

    /**
     * Cancel all payments
     * Uses a single operation to remove all items at once
     */
    fun cancelAllPayments() {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, nothing to cancel")
            return
        }

        dispatch(PaymentProcessingAction.ClearQueue)
    }

    /**
     * Abort the current processor
     */
    fun abortPayment(clearQueue: Boolean = false) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, nothing to abort")
            return
        }

        dispatch(PaymentProcessingAction.AbortCurrentPayment)

        if (clearQueue) {
            viewModelScope.launch {
                paymentQueue.clearQueue()
            }
        }
    }

    // Processor-Level Input Handling

    /**
     * Confirm customer receipt printing (processor-level input request)
     */
    fun confirmCustomerReceiptPrinting(requestId: String, shouldPrint: Boolean) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot confirm receipt printing")
            return
        }

        dispatch(PaymentProcessingAction.ConfirmCustomerReceiptPrinting(requestId, shouldPrint))
    }

    /**
     * Confirm merchant PIX key (processor-level input request)
     */
    fun confirmMerchantPixKey(requestId: String, pixKey: String) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot confirm PIX key")
            return
        }

        dispatch(PaymentProcessingAction.ConfirmMerchantPixKey(requestId, pixKey))
    }

    /**
     * Confirm merchant PIX has been paid (processor-level input request)
     */
    fun confirmMerchantPixHasBeenPaid(requestId: String, didPay: Boolean) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot confirm PIX payment")
            return
        }

        dispatch(PaymentProcessingAction.ConfirmMerchantPixHasBeenPaid(requestId, didPay))
    }

    // Queue-Level Input Handling

    /**
     * Confirm proceeding to the next processor (queue-level input request)
     */
    fun <T : QueueItem> confirmProcessor(requestId: String, modifiedItem: T) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot confirm processor")
            return
        }

        dispatch(PaymentProcessingAction.ConfirmProcessor(requestId, modifiedItem))
    }

    /**
     * Update processor type for all queued items
     * Used when toggling transactionless mode
     */
    fun toggleTransactionless(useTransactionless: Boolean) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot toggle transactionless")
            return
        }

        dispatch(PaymentProcessingAction.ToggleTransactionless(useTransactionless))
    }

    /**
     * Skip the current processor and move to the next one (queue-level input request)
     */
    fun skipProcessor(requestId: String) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot skip processor")
            return
        }

        dispatch(PaymentProcessingAction.SkipProcessor(requestId))
    }

    /**
     * Skip the current processor on error (for error retry dialogs)
     * This moves the item to the end of the queue for later retry
     */
    fun skipProcessorOnError(requestId: String) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot skip processor on error")
            return
        }

        dispatch(PaymentProcessingAction.SkipProcessorOnError(requestId))
    }

    // Error Handling

    /**
     * Handle a failed payment with the specified action (queue-level input request)
     *
     * @param requestId The ID of the input request
     * @param action The error handling action to take
     */
    private fun handleFailedPayment(requestId: String, action: ErrorHandlingAction) {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, cannot handle failed payment")
            return
        }

        dispatch(PaymentProcessingAction.HandleFailedPayment(requestId, action))
    }

    /**
     * Retry a failed payment immediately (queue-level input request)
     * This will retry the same processor without moving the item
     */
    fun retryPayment(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.RETRY)
    }

    /**
     * Retry a failed payment later (queue-level input request)
     * This will move the item to the end of the queue and continue with the next item
     */
    fun skipPayment(requestId: String) {
        handleFailedPayment(requestId, ErrorHandlingAction.SKIP)
    }

    /**
     * Abort all payments and clear the queue
     */
    fun abortAllPayments() {
        if (!::paymentQueue.isInitialized) {
            Log.w("PaymentProcessingViewModel", "Payment queue not initialized, nothing to abort")
            _paymentState.value = PaymentState.Cancelled
            return
        }

        viewModelScope.launch {
            try {
                dispatch(PaymentProcessingAction.AbortCurrentPayment)
                dispatch(PaymentProcessingAction.ClearQueue)
                _paymentState.value = PaymentState.Cancelled
                Log.d("PaymentProcessingViewModel", "All payments aborted and queue cleared")
            } catch (e: Exception) {
                Log.e("PaymentProcessingViewModel", "Error aborting payments", e)
                _paymentState.value = PaymentState.Error("Erro ao cancelar pagamentos")
            }
        }
    }
}