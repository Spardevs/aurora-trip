package br.com.ticpass.pos.core.queue.processors.payment.processors.impl

import android.util.Log
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.input.UserInputRequest
import br.com.ticpass.pos.core.queue.models.PaymentError
import br.com.ticpass.pos.core.queue.models.PaymentSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerPaymentEvent
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerPaymentMethod
import br.com.ticpass.pos.core.queue.processors.payment.AcquirerProcessingException
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingQueueItem
import br.com.ticpass.pos.core.queue.processors.payment.processors.core.PaymentProcessorBase
import br.com.ticpass.pos.core.queue.processors.payment.utils.SystemCustomerReceiptPrinting
import br.com.ticpass.utils.toMoney
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import br.com.uol.pagseguro.plugpagservice.wrapper.listeners.PlugPagPrintActionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PagSeguro Payment Processor
 * Processes payments using the PagSeguro PlugPag SDK via constructor injection.
 */
class AcquirerPaymentProcessor @Inject constructor(
    private val plugpag: PlugPag
) : PaymentProcessorBase() {
    
    private val tag = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var _item: PaymentProcessingQueueItem
    
    override suspend fun processPayment(item: PaymentProcessingQueueItem): ProcessingResult {
        try {
            _item = item
            setPaymentListener()
            setCustomerReceiptListener()

            val commission = (_item.amount * _item.commission).toMoney()

            val acquirerPaymentData = PlugPagPaymentData(
                AcquirerPaymentMethod.translate(_item.method),
                _item.amount + commission,
                PlugPag.INSTALLMENT_TYPE_A_VISTA,
                1,
                _item.id
            )

            val payment = withContext(Dispatchers.IO) {
                plugpag.doPayment(acquirerPaymentData)
            }
            val isSuccessful = payment.result == PlugPag.RET_OK

            if(!isSuccessful) throw AcquirerProcessingException(
                payment.errorCode,
                payment.result
            )

            clearPaymentListener()
            cleanupCoroutineScopes()

            Log.d("AcquirerRefundProcessor", "Payment processed successfully: ${payment.transactionCode}")
            Log.d("AcquirerRefundProcessor", "Payment processed successfully: ${payment.transactionId}")

            return PaymentSuccess(
                atk = payment.transactionCode ?: "",
                txId = payment.transactionId ?: "",
            )
        }
        catch (exception: AcquirerProcessingException) {
            return PaymentError(exception.event)
        }
        catch (exception: Exception) {
            return PaymentError(ProcessingErrorEvent.GENERIC)
        }
    }
    
    /**
     * PagSeguro-specific abort logic
     * Cancels any ongoing payment transaction and cleans up resources
     */
    override suspend fun onAbort(item: PaymentProcessingQueueItem?): Boolean {
        try {
            val abortResult = withContext(Dispatchers.IO) {
                plugpag.abort()
            }

            val hasAborted = abortResult.result == PlugPag.RET_OK
            if(!hasAborted) throw Exception("Failed to abort transaction.")

            clearPaymentListener()
            cleanupCoroutineScopes()

            return true
        } catch (e: Exception) {
            Log.e(tag, "Error aborting transaction: ${e.message}")
            return false
        }
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
     * Sets the payment event listener to handle payment events.
     * This listener will emit events to the _events channel.
     */
    private fun setPaymentListener() {
        var isOver = false

        val eventListener = object : PlugPagEventListener {
            override fun onEvent(data: PlugPagEventData) {
                if (data.eventCode == PlugPagEventData.EVENT_CODE_SALE_END) isOver = true
                if(isOver) return

                val event = AcquirerPaymentEvent.translate(data.eventCode)
                _events.tryEmit(event)
            }
        }

        plugpag.setEventListener(eventListener)
    }

    /**
     * Clears the payment event listener to stop receiving events.
     * This is important to avoid memory leaks and ensure that the
     * listener does not receive further events after the payment process is complete.
     */
    private fun clearPaymentListener() {
        val eventListener = object : PlugPagEventListener {
            override fun onEvent(data: PlugPagEventData) {
                // No-op, just to clear the listener
            }
        }
        plugpag.setEventListener(eventListener)
    }

    /**
     * Sets the customer receipt listener to handle customer receipt printing.
     * This listener will prompt the user for confirmation before printing the receipt
     * based on the configured SystemCustomerReceiptPrinting setting.
     */
    private fun setCustomerReceiptListener() {
        scope.launch {
            plugpag.setPrintActionListener(
                object : PlugPagPrintActionListener {
                    override fun onError(exception: PlugPagException?) {
                        exception?.toString()
                            ?.let { Log.d("$tag:setCustomerReceiptListener:onError", it) }
                    }

                    override fun onPrint(
                        phoneNumber: String?,
                        transactionResult: PlugPagTransactionResult?,
                        onFinishActions: PlugPagPrintActionListener.OnFinishPlugPagPrintActions?
                    ) {
                        when(_item.customerReceiptPrinting) {
                            SystemCustomerReceiptPrinting.CONFIRMATION -> {
                                try {
                                    scope.launch {
                                        val userAccepted = requestUserInput(
                                            UserInputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING()
                                        ).value as? Boolean ?: true

                                        if (userAccepted) onFinishActions?.doPrint(plugpag)
                                        else onFinishActions?.doNothing(plugpag)
                                    }
                                }
                                catch (e: Exception) {
                                    Log.e(
                                        "$tag:setCustomerReceiptListener:CONFIRMATION:Error",
                                        "${e.message}"
                                    )
                                    onFinishActions?.doPrint(plugpag)
                                }
                            }
                            SystemCustomerReceiptPrinting.AUTO -> {
                                onFinishActions?.doPrint(plugpag)
                            }
                            else -> {
                                onFinishActions?.doNothing(plugpag)
                            }
                        }
                    }
                }
            )
        }
    }
}