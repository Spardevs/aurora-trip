package br.com.ticpass.pos.core.queue.processors.refund.processors.impl

import android.util.Log
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.RefundError
import br.com.ticpass.pos.core.queue.models.RefundSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.refund.AcquirerRefundErrorEvent
import br.com.ticpass.pos.core.queue.processors.refund.AcquirerRefundException
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.core.queue.processors.refund.processors.core.RefundProcessorBase
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PagSeguro Refund Processor
 * Processes refunds using the PagSeguro PlugPag SDK via constructor injection.
 */
class AcquirerRefundProcessor @Inject constructor(
    private val plugpag: PlugPag
) : RefundProcessorBase() {

    private val tag = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: RefundQueueItem
    private lateinit var refundData: PlugPagVoidData

    override suspend fun processRefund(item: RefundQueueItem): ProcessingResult {
        try {
            _item = item

            _events.emit(RefundEvent.PROCESSING)

            refundData = PlugPagVoidData(
                transactionCode = item.atk,
                transactionId = item.txId,
                printReceipt = true,
                voidType = if(item.isQRCode) PlugPag.VOID_QRCODE else PlugPag.VOID_PAYMENT
            )

            _events.emit(RefundEvent.REFUNDING)

            withContext(Dispatchers.IO) {
                doRefund()
            }

            cleanupCoroutineScopes()

            return RefundSuccess()
        }
        catch (exception: AcquirerRefundException) {
            return RefundError(exception.event)
        }
        catch (exception: Exception) {
            Log.e(tag, "Generic refund error", exception)
            return RefundError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * PagSeguro-specific abort logic
     * Cancels any ongoing refund transaction
     */
    override suspend fun onAbort(item: RefundQueueItem?): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        try {
            scope.launch {
                plugpag.abort()
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
     * future refund operations.
     */
    private fun cleanupCoroutineScopes() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Performs the actual refund operation using the PlugPag SDK.
     */
    private fun doRefund() {
        try {
            val refund = plugpag.voidPayment(refundData)
            val isSuccessful = refund.result == PlugPag.RET_OK

            if(!isSuccessful) throw AcquirerRefundException(
                refund.errorCode,
                refund.result
            )
        }
        catch (exception: PlugPagException) {
            throw AcquirerRefundException(
                exception.errorCode,
                null
            )
        }
        catch (exception: AcquirerRefundException) {
            throw exception
        }
        catch (exception: Exception) {
            throw AcquirerRefundException(
                AcquirerRefundErrorEvent.GENERIC_RETRY_ERROR.code,
                null
            )
        }
    }
}