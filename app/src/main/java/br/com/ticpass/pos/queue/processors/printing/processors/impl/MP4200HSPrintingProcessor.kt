package br.com.ticpass.pos.queue.processors.printing.processors.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import br.com.ticpass.pos.printing.utils.ESCPOSPrinter
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.PrintingError
import br.com.ticpass.pos.queue.models.PrintingSuccess
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.printing.exceptions.PrintingException
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.core.PrintingProcessorBase
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrinterNetworkInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MP 4200 HS Printing Processor
 * Processes printings over network using the MP 4200 HS printer
 */
class MP4200HSPrintingProcessor : PrintingProcessorBase() {

    private val tag = this.javaClass.simpleName
    private val isAborting = AtomicBoolean(false)
    private lateinit var _item: PrintingQueueItem
    private lateinit var _bitmap: Bitmap
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun processPrinting(item: PrintingQueueItem): ProcessingResult {
        try {
            _item = item

            _events.tryEmit(PrintingEvent.PROCESSING)

            _bitmap = withContext(Dispatchers.IO) {
                readBitmapFromFile(_item.filePath)
            }

            val networkInfo = withContext(Dispatchers.IO) {
                requestPrinterNetworkInfo()
            }

            _events.tryEmit(PrintingEvent.PRINTING)

            withContext(Dispatchers.IO) {
                doPrint(_bitmap, networkInfo)
            }

            cleanup()

            return PrintingSuccess()
        }
        catch (exception: PrintingException) {
            return PrintingError(exception.error)
        }
        catch (exception: Exception) {
            return PrintingError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * MP 4200 HS abort logic
     * Cancels any ongoing printing and cleans up resources
     */
    override suspend fun onAbort(item: PrintingQueueItem?): Boolean {
        return try {
            isAborting.set(true)
            cleanup()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cleans up resources used by the processor.
     * This includes cancelling coroutines and recycling the bitmap if it was initialized.
     */
    private fun cleanup() {
        cleanupCoroutineScopes()
        if(::_bitmap.isInitialized) _bitmap.recycle()
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
     * Reads a bitmap from the specified file path.
     */
    private fun readBitmapFromFile(filePath: String): Bitmap {
        try {
            val bitmap =  BitmapFactory.decodeFile(filePath)
            return bitmap
        }
        catch (exception: NullPointerException) {
            throw PrintingException(
                ProcessingErrorEvent.PRINT_FILE_NOT_FOUND
            )
        }
        catch (exception: Exception) {
            throw PrintingException(
                ProcessingErrorEvent.GENERIC
            )
        }
    }

    /**
     * Requests the printer network information from the user.
     * If the user does not provide valid information, it defaults to a common
     * IP address and port for the MP 4200 HS printer.
     */
    private suspend fun requestPrinterNetworkInfo(): PrinterNetworkInfo {
        try {
            val printerInfo = requestUserInput(
                UserInputRequest.CONFIRM_PRINTER_NETWORK_INFO()
            ).value as? PrinterNetworkInfo ?: PrinterNetworkInfo("192.168.0.2", 9100)

            return printerInfo
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

    /**
     * Performs the actual printing operation using the ESCPOSPrinter utility.
     */
    private suspend fun doPrint(bitmap: Bitmap, networkInfo: PrinterNetworkInfo) {
        try {
            val printer = ESCPOSPrinter(
                networkInfo.ipAddress,
                networkInfo.port
            )

            printer.connect()
            printer.sendImage(bitmap)
            printer.cut()
            printer.disconnect()
        }
        catch (exception: Exception) {
            throw PrintingException(
                ProcessingErrorEvent.GENERIC
            )
        }
    }
}
