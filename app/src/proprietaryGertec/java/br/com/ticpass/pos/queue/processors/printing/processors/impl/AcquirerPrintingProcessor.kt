package br.com.ticpass.pos.queue.processors.printing.processors.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import br.com.gertec.easylayer.printer.Alignment
import br.com.gertec.easylayer.printer.PrintConfig
import br.com.gertec.easylayer.printer.Printer
import br.com.gertec.easylayer.printer.PrinterError
import br.com.gertec.easylayer.printer.PrinterException
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.models.PrintingError
import br.com.ticpass.pos.queue.models.PrintingSuccess
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.printing.AcquirerPrintingException
import br.com.ticpass.pos.queue.processors.printing.exceptions.PrintingException
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.core.PrintingProcessorBase
import br.com.ticpass.pos.sdk.AcquirerSdk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gertec Printing Processor
 * Performs printing using the Gertec EasyLayer SDK
 */
class AcquirerPrintingProcessor : PrintingProcessorBase() {

    private val tag = this.javaClass.simpleName
    private val printingProviderFactory = AcquirerSdk.printing.getInstance()
    private lateinit var _item: PrintingQueueItem
    private lateinit var printer: Printer
    private lateinit var printerUtils: br.com.gertec.easylayer.printer.PrinterUtils
    private lateinit var _bitmap: Bitmap
    private var _monochromaticBitmap: Bitmap? = null
    private var currentListener: Printer.Listener? = null

    override suspend fun processPrinting(item: PrintingQueueItem): ProcessingResult {
        val deferred = CompletableDeferred<ProcessingResult>()
        
        try {
            _item = item

            // Create unique listener for this specific print job
            val listener = createPrinterListener(deferred)

            // Factory creates provider with this job's unique callback
            val provider = printingProviderFactory(listener)
            printer = provider.printer
            printerUtils = provider.printerUtils

            _bitmap = withContext(Dispatchers.IO) {
                readBitmapFromFile(_item.filePath)
            }

            withContext(Dispatchers.IO) {
                doPrint(_bitmap)
            }

        } catch (exception: PrintingException) {
            deferred.complete(PrintingError(exception.error))
        } catch (exception: AcquirerPrintingException) {
            deferred.complete(PrintingError(exception.event))
        } catch (exception: Exception) {
            Log.e(tag, "Unexpected error during printing", exception)
            deferred.complete(PrintingError(ProcessingErrorEvent.GENERIC))
        }

        val result = deferred.await()
        cleanup()
        return result
    }

    /**
     * Gertec-specific abort logic
     * Currently no direct abort method in Gertec SDK
     */
    override suspend fun onAbort(item: PrintingQueueItem?): Boolean {
        cleanup()
        return true
    }

    /**
     * Reads bitmap from file path
     */
    private fun readBitmapFromFile(filePath: String): Bitmap {
        try {
            val bitmap = BitmapFactory.decodeFile(filePath)
            return bitmap ?: throw PrintingException(ProcessingErrorEvent.PRINT_FILE_NOT_FOUND)
        } catch (exception: NullPointerException) {
            throw PrintingException(ProcessingErrorEvent.PRINT_FILE_NOT_FOUND)
        } catch (exception: Exception) {
            Log.e(tag, "Unexpected error during bitmap reading", exception)
            throw PrintingException(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Performs the actual printing operation using Gertec Printer
     */
    private suspend fun doPrint(bitmap: Bitmap) {
        try {
            // Convert to monochromatic for better printing quality
            _monochromaticBitmap = printerUtils.toMonochromatic(bitmap, 0.4)

            // Configure print settings
            val printConfig = PrintConfig()
            printConfig.width = 384  // Standard thermal printer width
            printConfig.alignment = Alignment.CENTER

            _events.tryEmit(PrintingEvent.PRINTING)

            // Execute print using the printer instance with unique callback
            // NOTE: printImage() is asynchronous - bitmap will be used in background thread
            // Do NOT recycle the bitmap here! It will be recycled in cleanup()
            printer.printImage(printConfig, _monochromaticBitmap!!)
            printer.scrollPaper(2)

        } catch (exception: PrinterException) {
            Log.e(tag, "PrinterException: ${exception.message}", exception)
            throw AcquirerPrintingException(ProcessingErrorEvent.PRINTER_ERROR)
        }
    }

    /**
     * Creates a unique Printer.Listener for this specific print job
     * 
     * @param deferred The CompletableDeferred to complete when print finishes
     * @return A Printer.Listener that handles success and error callbacks
     */
    private fun createPrinterListener(deferred: CompletableDeferred<ProcessingResult>): Printer.Listener {
        val listener = object : Printer.Listener {
            override fun onPrinterError(printerError: PrinterError) {
                Log.e(tag, "Printer error: ${printerError.cause}")
                val exception = AcquirerPrintingException(printerError.cause ?: "UNKNOWN_ERROR")
                deferred.complete(PrintingError(exception.event))
            }

            override fun onPrinterSuccessful(printerRequestId: Int) {
                Log.d(tag, "Print successful: $printerRequestId")
                deferred.complete(PrintingSuccess())
            }
        }
        
        currentListener = listener
        return listener
    }

    /**
     * Cleans up the printer listener by nullifying the reference
     * This helps with garbage collection and prevents callback leaks
     */
    private fun cleanupListener() {
        currentListener = null
    }

    /**
     * Cleans up resources used by the processor
     */
    private fun cleanup() {
        // Recycle original bitmap
        if (::_bitmap.isInitialized && !_bitmap.isRecycled) {
            _bitmap.recycle()
        }
        
        // Recycle monochromatic bitmap (created during print)
        _monochromaticBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
        _monochromaticBitmap = null
        
        cleanupListener()
    }
}
