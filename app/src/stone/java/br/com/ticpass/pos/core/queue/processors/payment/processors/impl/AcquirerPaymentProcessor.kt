package br.com.ticpass.pos.core.queue.processors.payment.processors.impl

import br.com.stone.posandroid.providers.PosPrintReceiptProvider
import br.com.stone.posandroid.providers.PosTransactionProvider
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.input.UserInputRequest
import br.com.ticpass.pos.core.queue.models.PaymentError
import br.com.ticpass.pos.core.queue.models.PaymentSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerPaymentAction
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerPaymentActionCode
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerPaymentActionCodeError
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerPaymentMethod
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerPaymentProcessingException
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerPaymentStatusError
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.core.queue.processors.payment.processors.core.PaymentProcessorBase
import br.com.ticpass.pos.core.queue.processors.payment.utils.SystemCustomerReceiptPrinting
import br.com.ticpass.pos.core.sdk.AcquirerSdk
import br.com.ticpass.utils.toMoney
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stone.application.enums.Action
import stone.application.enums.ErrorsEnum
import stone.application.enums.InstalmentTransactionEnum
import stone.application.interfaces.StoneActionCallback
import stone.application.interfaces.StoneCallbackInterface
import stone.database.transaction.TransactionObject
import javax.inject.Inject

/**
 * Stone Payment Processor
 * Processes payments using the Stone SDK via direct SDK access.
 */
class AcquirerPaymentProcessor @Inject constructor() : PaymentProcessorBase() {

    private val tag = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var transactionData: TransactionObject
    private lateinit var paymentProvider: PosTransactionProvider
    private lateinit var customerReceiptProvider: PosPrintReceiptProvider
    private lateinit var _item: PaymentProcessingQueueItem

    override suspend fun processPayment(item: PaymentProcessingQueueItem): ProcessingResult {
        try {
            _item = item
            val commission = (_item.amount * _item.commission).toMoney()

            transactionData = TransactionObject()
            transactionData.typeOfTransaction = AcquirerPaymentMethod.translate(_item.method)
            transactionData.instalmentTransaction = InstalmentTransactionEnum.ONE_INSTALMENT
            transactionData.amount = (commission + _item.amount).toString()
            transactionData.isCapture = true
            transactionData.externalId = _item.id

            val (transactionProviderFactory, customerReceiptProviderFactory) = AcquirerSdk.payment.getInstance()
            paymentProvider = transactionProviderFactory(transactionData)
            customerReceiptProvider = customerReceiptProviderFactory(transactionData)

            val result = execTransaction()
            if(result is PaymentSuccess) printCustomerReceipt()

            cleanupCoroutineScopes()

            return result
        }
        catch (exception: AcquirerPaymentProcessingException) {
            return PaymentError(exception.event)
        }
        catch (exception: Exception) {
            return PaymentError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Stone-specific abort logic
     * Cancels any ongoing payment transaction
     */
    override suspend fun onAbort(item: PaymentProcessingQueueItem?): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        try {
            scope.launch {
                if(::paymentProvider.isInitialized) paymentProvider.abortPayment()
                cleanupCoroutineScopes()
            }
            deferred.complete(true)
        }
        catch (exception: Exception) { deferred.complete(false) }

        return deferred.await()
    }

    /**
     * Cancels all coroutines in the current scope and creates a new scope.
     * This ensures that any ongoing operations are properly terminated and
     * resources are released, while maintaining the processor ready for
     * future payment operations.
     */
    private fun cleanupCoroutineScopes() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Initializes the transaction with provider.
     * Sets up the connection callback to handle progress, success and error cases.
     *
     * @return ProcessingResult indicating success or error.
     */
    private suspend fun execTransaction(): ProcessingResult = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<ProcessingResult>()
        
        paymentProvider.setConnectionCallback(object : StoneActionCallback {
            override fun onSuccess() {
                try {
                    scope.launch {
                        val result = handleTransactionDone()
                        deferred.complete(result)
                    }
                }
                catch (e: AcquirerPaymentProcessingException) {
                    val exception = PaymentError(e.event)
                    deferred.complete(exception)
                }
                catch (e: Exception) {
                    val exception = PaymentError(ProcessingErrorEvent.GENERIC)
                    deferred.complete(exception)
                }
            }

            override fun onError() {
                try {
                    val error = paymentProvider.listOfErrors?.last() ?: ErrorsEnum.UNKNOWN_ERROR
                    throw AcquirerPaymentProcessingException(error)
                }
                catch (e: AcquirerPaymentProcessingException) {
                    val exception = PaymentError(e.event)
                    deferred.complete(exception)
                }
                catch (e: Exception) {
                    val exception = PaymentError(ProcessingErrorEvent.GENERIC)
                    deferred.complete(exception)
                }
            }

            override fun onStatusChanged(action: Action) {
                val event = AcquirerPaymentAction.translate(action)

                val processedEvent = when(event) {
                    is PaymentProcessingEvent.QRCODE_SCAN -> {
                        val qrCode = transactionData.qrCode
                        PaymentProcessingEvent.QRCODE_SCAN(qrCode, 90000L)
                    }
                    else -> { event }
                }

                _events.tryEmit(processedEvent)
            }
        })

        paymentProvider.execute()
        return@withContext deferred.await()
    }

    /**
     * Handles the transaction completion by checking the status and errors.
     * Emits the appropriate event and returns the processing result.
     *
     * @return ProcessingResult indicating success or error.
     */
    private fun handleTransactionDone(): ProcessingResult {
        try {
            val status = paymentProvider.transactionStatus
            val listOfErrors = paymentProvider.listOfErrors ?: emptyList()
            val isError = listOfErrors.isNotEmpty() || AcquirerPaymentStatusError.isError(status)
            val nullableActionCode = transactionData.actionCode == null

            if (isError) {
                val errorEvent = AcquirerPaymentActionCodeError.translate(transactionData.actionCode)
                throw AcquirerPaymentProcessingException(errorEvent)
            }

            // sometimes the action code is null (bug?), so we need to handle it
            val successEvent = if (nullableActionCode) PaymentProcessingEvent.APPROVAL_SUCCEEDED
            else AcquirerPaymentActionCode.translate(transactionData.actionCode)

            _events.tryEmit(successEvent)

            return PaymentSuccess(
                atk = transactionData.acquirerTransactionKey,
                txId = ""
            )
        }
        catch (e: AcquirerPaymentProcessingException) {
            val exception = PaymentError(e.event)
            return exception
        }
        catch (e: Exception) {
            val exception = PaymentError(ProcessingErrorEvent.GENERIC)
            return exception
        }
    }

    /**
     * Wrapper function to handle the printing of customer receipt
     * based on user demand or system configuration.
     */
    private suspend fun printCustomerReceipt(): Unit = withContext(Dispatchers.IO + SupervisorJob()) {
        when(_item.customerReceiptPrinting) {
            SystemCustomerReceiptPrinting.CONFIRMATION -> {
                val userAccepted = requestUserInput(
                    UserInputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING()
                ).value as? Boolean ?: true

                if (userAccepted) doPrintCustomerReceipt()
            }
            SystemCustomerReceiptPrinting.AUTO -> {
                doPrintCustomerReceipt()
            }
            SystemCustomerReceiptPrinting.NONE -> {}
        }
    }

    private suspend fun doPrintCustomerReceipt(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        
        customerReceiptProvider.connectionCallback = object : StoneCallbackInterface {
            override fun onSuccess() {
                deferred.complete(true)
            }

            override fun onError() {
                deferred.complete(false)
            }
        }

        customerReceiptProvider.execute()
        return deferred.await()
    }
}