package br.com.ticpass.pos.presentation.payment.coordination

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.payment.viewmodel.PaymentProcessingViewModel
import br.com.ticpass.pos.presentation.payment.states.PaymentProcessingUiState
import br.com.ticpass.pos.presentation.payment.dialogs.PaymentDialogManager
import br.com.ticpass.pos.presentation.payment.events.PaymentEventHandler
import br.com.ticpass.pos.core.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.presentation.payment.utils.PaymentUIUtils
import br.com.ticpass.pos.presentation.payment.view.PaymentProcessingQueueView
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEventResourceMapper
import br.com.ticpass.pos.core.queue.models.PaymentSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingState
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.utils.toMoneyAsDouble
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Coordinates all ViewModel observations and UI updates for the PaymentProcessingActivity.
 * This class handles the complex coordination logic, allowing the activity to focus on
 * lifecycle management and basic view setup.
 */
class PaymentActivityCoordinator(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val paymentViewModel: PaymentProcessingViewModel,
    private val dialogManager: PaymentDialogManager,
    private val eventHandler: PaymentEventHandler,
    private val queueView: PaymentProcessingQueueView,
    private val queueTitleTextView: TextView,
    private val dialogProgressTextView: TextView,
    private val dialogProgressBar: ProgressBar,
    private val dialogEventTextView: TextView,
    private val dialogPaymentMethodTextView: TextView,
    private val dialogPaymentAmountTextView: TextView,
    private val showProgressDialog: () -> Unit,
    private val hideProgressDialog: () -> Unit
) {

    private var totalPayments = 0
    private var currentProcessingIndex = 0

    /**
     * Initialize all ViewModel observations and UI coordination
     */
    fun initialize() {
        observeQueueState()
        observeUiEvents()
        observeProcessingState()
        observePaymentEvents()
        observeUiState()
    }

    private fun observeQueueState() {
        lifecycleScope.launch {
            paymentViewModel.queueState.collectLatest { queueItems ->
                updateQueueUI(queueItems ?: emptyList())
            }
        }
    }

    private fun observeUiEvents() {
        lifecycleScope.launch {
            paymentViewModel.uiEvents.collect { event ->
                eventHandler.handleUiEvent(event)
            }
        }
    }

    private fun observeProcessingState() {
        lifecycleScope.launch {
            paymentViewModel.processingState.collectLatest { state ->
                if (state is ProcessingState.ItemProcessing<*> ||
                    state is ProcessingState.ItemRetrying<*>) {
                    showProgressDialog()
                }

                when (state) {
                    is ProcessingState.ItemProcessing<*> -> {
                        val item = state.item as? PaymentProcessingQueueItem
                        if (item != null) {
                            updateProcessingProgress(paymentViewModel.currentIndex as Int,
                                paymentViewModel.fullSize as Int
                            )
                            updatePaymentInfo(item)
                        }
                    }
                    is ProcessingState.ItemDone<*> -> {
                        var teste = state.result as PaymentSuccess

                        Log.d("Teste", "${state.item}")
                        Log.d("Teste", "${teste.txId}")
                    }
                    is ProcessingState.ItemFailed<*> -> {
                        val error = state.error
                        val resourceId = ProcessingErrorEventResourceMapper.getErrorResourceKey(error)
                        val displayMessage = context.getString(resourceId)

                        displayErrorMessage(error)
                    }
                    is ProcessingState.ItemRetrying<*> -> {
                        val item = state.item as? PaymentProcessingQueueItem
                        if (item != null) {
                            updateProcessingProgress(paymentViewModel.currentIndex as Int,
                                paymentViewModel.fullSize as Int
                            )
                            updatePaymentInfo(item)
                        }
                    }
                    is ProcessingState.ItemSkipped<*> -> {
                        updateProcessingProgress(paymentViewModel.currentIndex as Int,
                            paymentViewModel.fullSize as Int
                        )
                    }
                    is ProcessingState.QueueCanceled<*> -> {
                        updateProcessingProgress(0, 0)
                    }
                    is ProcessingState.QueueDone<*> -> {
                        updateProcessingProgress(0, 0)
                    }
                    else -> {
                        updateProcessingProgress(0, 0)
                    }
                }
            }
        }
    }

    private fun observePaymentEvents() {
        lifecycleScope.launch {
            paymentViewModel.paymentProcessingEvents.collectLatest { event ->
                eventHandler.handlePaymentEvent(event)
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            paymentViewModel.uiState.collectLatest { uiState ->
                when (uiState) {
                    is PaymentProcessingUiState.Error -> {
                        displayErrorMessage(uiState.event)
                    }
                    is PaymentProcessingUiState.ConfirmNextProcessor<*> -> {
                        dialogManager.showConfirmNextPaymentProcessorDialog(uiState.requestId)
                    }
                    is PaymentProcessingUiState.ConfirmCustomerReceiptPrinting -> {
                        dialogManager.showCustomerReceiptDialog(uiState.requestId, uiState.timeoutMs)
                    }
                    is PaymentProcessingUiState.MerchantPixScanning -> {
                        dialogManager.showPixScanningDialog(uiState.requestId, uiState.pixCode)
                    }
                    is PaymentProcessingUiState.ConfirmMerchantPixKey -> {
                        confirmMerchantPixKey(uiState.requestId)
                    }
                    is PaymentProcessingUiState.ErrorRetryOrSkip -> {
                        dialogManager.showErrorRetryOptionsDialog(uiState.requestId, uiState.error)
                    }
                    else -> {
                        Log.d("PaymentActivityCoordinator", "No dialog needed for state: $uiState")
                    }
                }
            }
        }
    }

    private fun updateQueueUI(queueItems: List<PaymentProcessingQueueItem>) {
        queueView.updateQueue(queueItems)
        totalPayments = queueItems.size

        val formattedTitle = String.format(context.getString(R.string.payment_queue), queueItems.size)
        queueTitleTextView.text = formattedTitle
    }

    private fun updateProcessingProgress(current: Int, total: Int) {
        currentProcessingIndex = current

        if(total == 1) {
            dialogProgressTextView.text = context.getString(R.string.payment_progress_first)
        } else {
            dialogProgressTextView.text = context.getString(R.string.payment_progress, current, total)
        }
        dialogProgressBar.progress = current
        dialogProgressBar.max = total

        if (total > 0) {
            showProgressDialog()
        } else {
            hideProgressDialog()
        }
    }

    private fun displayErrorMessage(error: ProcessingErrorEvent) {
        val errorMessage = PaymentUIUtils.getErrorMessage(context, error)
        PaymentUIUtils.logError("PaymentActivityCoordinator", error, context)

        dialogEventTextView.text = errorMessage

        showProgressDialog()
    }

    private fun updatePaymentInfo(item: PaymentProcessingQueueItem) {
        val paymentMethodDisplayName = getPaymentMethodDisplayName(item.method)
        dialogPaymentMethodTextView.text = paymentMethodDisplayName

        val amountInReais = item.amount.toMoneyAsDouble()
        val formattedAmount = String.format("R$ %.2f", amountInReais)
        dialogPaymentAmountTextView.text = formattedAmount
    }

    private fun getPaymentMethodDisplayName(method: SystemPaymentMethod): String {
        return when (method) {
            SystemPaymentMethod.CREDIT -> context.getString(R.string.enqueue_credit_payment)
            SystemPaymentMethod.DEBIT -> context.getString(R.string.enqueue_debit_payment)
            SystemPaymentMethod.VOUCHER -> context.getString(R.string.enqueue_voucher_payment)
            SystemPaymentMethod.PIX -> context.getString(R.string.enqueue_pix_payment)
            SystemPaymentMethod.MERCHANT_PIX -> context.getString(R.string.enqueue_personal_pix_payment)
            SystemPaymentMethod.CASH -> context.getString(R.string.enqueue_cash_payment)
            SystemPaymentMethod.LN_BITCOIN -> context.getString(R.string.enqueue_bitcoin_ln_payment)
        }
    }

    private fun confirmMerchantPixKey(requestId: String) {
        val pixKey = PaymentUIUtils.getHardcodedPixKey()

        paymentViewModel.confirmMerchantPixKey(requestId, pixKey)
    }
}