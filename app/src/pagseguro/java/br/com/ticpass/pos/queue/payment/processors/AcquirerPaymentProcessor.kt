package br.com.ticpass.pos.queue.payment.processors

import android.util.Log
import br.com.ticpass.Constants.CONVERSION_FACTOR
import br.com.ticpass.pos.queue.InputRequest
import br.com.ticpass.pos.queue.ProcessingErrorEvent
import br.com.ticpass.pos.queue.ProcessingResult
import br.com.ticpass.pos.queue.payment.AcquirerPaymentEvent
import br.com.ticpass.pos.queue.payment.AcquirerPaymentMethod
import br.com.ticpass.pos.queue.payment.AcquirerProcessingException
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueItem
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import br.com.ticpass.pos.queue.payment.SystemCustomerReceiptPrinting
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import br.com.uol.pagseguro.plugpagservice.wrapper.listeners.PlugPagPrintActionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * PagSeguro Payment Processor
 * Processes payments using the acquirer SDK
 */
class AcquirerPaymentProcessor : PaymentProcessorBase() {

    private val tag = "AcquirerPaymentProcessor"
    private val plugpag = AcquirerSdk.payment.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var _item: ProcessingPaymentQueueItem
    
    override suspend fun processPayment(item: ProcessingPaymentQueueItem): ProcessingResult {
        try {
            _item = item
            setPaymentListener()
            setCustomerReceiptListener()

            val commission = ((item.amount * item.commission) / CONVERSION_FACTOR).toInt()

            val acquirerPaymentData = PlugPagPaymentData(
                AcquirerPaymentMethod.translate(item.method),
                item.amount + commission,
                PlugPag.INSTALLMENT_TYPE_A_VISTA,
                1,
                item.id
            )

            // Use withContext to ensure this blocking call doesn't freeze the UI
            val payment = kotlinx.coroutines.withContext(Dispatchers.IO) {
                plugpag.doPayment(acquirerPaymentData)
            }
            val isSuccessful = payment.result == PlugPag.RET_OK

            if(!isSuccessful) throw AcquirerProcessingException(
                payment.errorCode,
                payment.result
            )

            return ProcessingResult.Success(
                atk = "",
                txId =  ""
            )
        }
        catch (exception: Exception) {
            if (exception is AcquirerProcessingException) {
                return ProcessingResult.Error(exception.event)
            }

            return ProcessingResult.Error(ProcessingErrorEvent.GENERIC)
        }
        finally {
            clearPaymentListener()
            cleanupCoroutineScopes()
        }
    }

    private fun cleanupCoroutineScopes() {
        scope.cancel()
    }
    
    /**
     * PagSeguro-specific abort logic
     * Cancels any ongoing payment transaction and cleans up resources
     */
    override suspend fun onAbort(item: ProcessingPaymentQueueItem?): Boolean {
        try {
            // Attempt to abort any ongoing transaction
            val abortResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                plugpag.abort()
            }

            val hasAborted = abortResult.result == PlugPag.RET_OK

            clearPaymentListener()
            cleanupCoroutineScopes()

            return hasAborted
        } catch (e: Exception) {
            Log.e(tag, "Error aborting transaction: ${e.message}")
            return false
        }
    }

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

    private fun clearPaymentListener() {
        val eventListener = object : PlugPagEventListener {
            override fun onEvent(data: PlugPagEventData) {
                // No-op, just to clear the listener
            }
        }
        plugpag.setEventListener(eventListener)
    }


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
                                        val response = requestInput(
                                            InputRequest.CONFIRM_CUSTOMER_RECEIPT_PRINTING()
                                        )

                                        if (response.value == true) onFinishActions?.doPrint(plugpag)
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