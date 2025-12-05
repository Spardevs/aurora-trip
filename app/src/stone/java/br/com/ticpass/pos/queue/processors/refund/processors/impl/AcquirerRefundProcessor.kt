package br.com.ticpass.pos.core.queue.processors.refund.processors.impl

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.RefundError
import br.com.ticpass.pos.core.queue.models.RefundSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.refund.AcquirerRefundActionCode
import br.com.ticpass.pos.core.queue.processors.refund.AcquirerRefundActionCodeError
import br.com.ticpass.pos.core.queue.processors.refund.AcquirerRefundException
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundEvent
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueItem
import br.com.ticpass.pos.core.queue.processors.refund.processors.core.RefundProcessorBase
import br.com.ticpass.pos.core.sdk.factory.AcquirerRefundProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stone.application.enums.ErrorsEnum
import stone.application.interfaces.StoneCallbackInterface
import stone.database.transaction.TransactionObject
import stone.providers.CancellationProvider
import javax.inject.Inject

/**
 * Stone Refund Processor
 * Processes refunds using the Stone SDK via constructor injection.
 */
class AcquirerRefundProcessor @Inject constructor(
    private val refundProviderFactory: AcquirerRefundProvider
) : RefundProcessorBase() {

    private val tag = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: RefundQueueItem
    private lateinit var refundProvider: CancellationProvider
    private lateinit var transactionData: TransactionObject

    override suspend fun processRefund(item: RefundQueueItem): ProcessingResult {
        try {
            _item = item

            val providers = refundProviderFactory(item.atk)
            refundProvider = providers.first
            transactionData = providers.second

            val result = withContext(Dispatchers.IO) {
                doRefund()
            }

            cleanup()

            return result
        }
        catch (exception: Exception) {
            return RefundError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Stone-specific abort logic
     * Cancels any ongoing refund transaction
     */
    override suspend fun onAbort(item: RefundQueueItem?): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        try {
            scope.launch {
                cleanup()
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
     * Cleans up resources used by the processor.
     * This includes cancelling coroutines and recycling the bitmap if it was initialized.
     */
    private fun cleanup() {
        cleanupCoroutineScopes()
    }

    private suspend fun doRefund(): ProcessingResult {
        val deferred = CompletableDeferred<ProcessingResult>()

        refundProvider.connectionCallback = object : StoneCallbackInterface {
            override fun onSuccess() {
                try {
                    scope.launch {
                        val result = handleRefundDone()
                        deferred.complete(result)
                    }
                } catch (e: AcquirerRefundException) {
                    val exception = RefundError(e.event)
                    deferred.complete(exception)
                } catch (e: Exception) {
                    val exception = RefundError(ProcessingErrorEvent.GENERIC)
                    deferred.complete(exception)
                }
            }

            override fun onError() {
                try {
                    val error = refundProvider.listOfErrors?.last() ?: ErrorsEnum.UNKNOWN_ERROR
                    throw AcquirerRefundException(error)
                } catch (e: AcquirerRefundException) {
                    val exception = RefundError(e.event)
                    deferred.complete(exception)
                } catch (e: Exception) {
                    val exception = RefundError(ProcessingErrorEvent.GENERIC)
                    deferred.complete(exception)
                }
            }
        }

        _events.tryEmit(RefundEvent.REFUNDING)

        refundProvider.execute()

        return deferred.await()
    }

    private fun handleRefundDone(): ProcessingResult {
        try {
            val listOfErrors = refundProvider.listOfErrors ?: emptyList()
            val isError = listOfErrors.isNotEmpty()
            val nullableActionCode =
                transactionData.actionCode == null || transactionData.actionCode.isBlank()

            if (isError) {
                val errorEvent = AcquirerRefundActionCodeError.translate(transactionData.actionCode)
                throw AcquirerRefundException(errorEvent)
            }

            // sometimes the action code is null (bug?), so we need to handle it
            val successEvent = if (nullableActionCode) RefundEvent.SUCCESS
            else AcquirerRefundActionCode.translate(transactionData.actionCode)

            _events.tryEmit(successEvent)

            return RefundSuccess()
        }
        catch (e: AcquirerRefundException) {
            val exception = RefundError(e.event)
            return exception
        }
        catch (e: Exception) {
            val exception = RefundError(ProcessingErrorEvent.GENERIC)
            return exception
        }
    }
}