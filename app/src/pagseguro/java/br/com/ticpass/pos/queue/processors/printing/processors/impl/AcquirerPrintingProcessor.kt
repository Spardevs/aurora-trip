package br.com.ticpass.pos.queue.processors.printing.processors.impl

import android.util.Log
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.PrintingError
import br.com.ticpass.pos.queue.models.PrintingSuccess
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.printing.AcquirerPrintingErrorEvent
import br.com.ticpass.pos.queue.processors.printing.AcquirerPrintingException
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.core.PrintingProcessorBase
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrinterData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PagSeguro Printing Processor
 * Do printings using the acquirer SDK
 */
class AcquirerPrintingProcessor : PrintingProcessorBase() {

    private val tag = this.javaClass.simpleName
    private val plugpag = AcquirerSdk.printing.getInstance()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: PrintingQueueItem

    override suspend fun processPrinting(item: PrintingQueueItem): ProcessingResult {
        try {
            _item = item

            _events.emit(PrintingEvent.PROCESSING)

            val printerData = PlugPagPrinterData(
                filePath = item.filePath,
                4,
                0
            )

            _events.emit(PrintingEvent.PRINTING)

            withContext(Dispatchers.IO) {
                doPrintFile(printerData)
            }

            cleanupCoroutineScopes()

            return PrintingSuccess()
        }
        catch (exception: AcquirerPrintingException) {
            return PrintingError(exception.event)
        }
        catch (exception: Exception) {
            Log.e(tag, "Generic printing error", exception)
            return PrintingError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * PagSeguro-specific abort logic
     * Cancels any ongoing printing transaction
     */
    override suspend fun onAbort(item: PrintingQueueItem?): Boolean {
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
     * future printing operations.
     */
    private fun cleanupCoroutineScopes() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Performs the actual printing operation using the PlugPag SDK.
     */
    private fun doPrintFile(printerData: PlugPagPrinterData) {
        try {
            val printing = plugpag.printFromFile(printerData)
            val isSuccessful = printing.result == PlugPag.RET_OK

            if(!isSuccessful) throw AcquirerPrintingException(
                printing.errorCode,
                printing.result
            )
        }
        catch (exception: PlugPagException) {
            throw AcquirerPrintingException(
                exception.errorCode,
                null
            )
        }
        catch (exception: AcquirerPrintingException) {
            throw exception
        }
        catch (exception: Exception) {
            throw AcquirerPrintingException(
                AcquirerPrintingErrorEvent.GENERIC_RETRY_ERROR.code,
                null
            )
        }
    }
}