package br.com.ticpass.pos.core.queue.processors.printing.processors.impl

import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.PrintingError
import br.com.ticpass.pos.core.queue.models.PrintingSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.printing.AcquirerPrintingException
import br.com.ticpass.pos.core.queue.processors.printing.exceptions.PrintingException
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.core.queue.processors.printing.processors.core.PrintingProcessorBase
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * MPT II Printing Processor
 * Processes printings using the MPT II printer.
 */
class MPTIIPrintingProcessor @Inject constructor() : PrintingProcessorBase() {

    private val tag = this.javaClass.simpleName
    private val isAborting = AtomicBoolean(false)
    private lateinit var _item: PrintingQueueItem
    
    override suspend fun processPrinting(item: PrintingQueueItem): ProcessingResult {
        try {
            _item = item

            return PrintingSuccess()
        }
        catch (exception: PrintingException) {
            throw exception
        }
        catch (exception: Exception) {
            throw PrintingException(
                ProcessingErrorEvent.GENERIC
            )
        }
    }

    override suspend fun onAbort(item: PrintingQueueItem?): Boolean {
        return try {
            isAborting.set(true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
