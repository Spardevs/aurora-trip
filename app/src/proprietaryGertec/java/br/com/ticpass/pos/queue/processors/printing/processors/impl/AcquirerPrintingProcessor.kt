package br.com.ticpass.pos.queue.processors.printing.processors.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import br.com.gertec.easylayer.printer.Alignment
import br.com.gertec.easylayer.printer.CutType
import br.com.gertec.easylayer.printer.PrintConfig
import br.com.gertec.easylayer.printer.Printer
import br.com.gertec.easylayer.printer.PrinterError
import br.com.gertec.easylayer.printer.PrinterException
import br.com.ticpass.pos.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.queue.input.UserInputRequest
import br.com.ticpass.pos.queue.models.PrintingError
import br.com.ticpass.pos.queue.models.PrintingSuccess
import br.com.ticpass.pos.queue.models.ProcessingResult
import br.com.ticpass.pos.queue.processors.printing.AcquirerPrintingException
import br.com.ticpass.pos.queue.processors.printing.exceptions.PrintingException
import br.com.ticpass.pos.queue.processors.printing.models.PaperCutType
import br.com.ticpass.pos.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.queue.processors.printing.processors.core.PrintingProcessorBase
import br.com.ticpass.pos.sdk.AcquirerSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Gertec Printing Processor
 * Performs printing using the Gertec EasyLayer SDK
 */
class AcquirerPrintingProcessor : PrintingProcessorBase() {

    private val tag = this.javaClass.simpleName
    private val printingProviderFactory = AcquirerSdk.printing.getInstance()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: PrintingQueueItem
    private lateinit var printer: Printer
    private lateinit var context: Context
    private lateinit var _bitmap: Bitmap

    override suspend fun processPrinting(item: PrintingQueueItem): ProcessingResult {
        try {
            _item = item

            // Create mock bitmap for testing if file path is empty or doesn't exist
            if (_item.filePath.isEmpty() || !java.io.File(_item.filePath).exists()) {
                _item = _item.copy(filePath = "mock_hello_world_bitmap")
            }

            // Factory creates provider - no listener needed for synchronous operations
            val provider = printingProviderFactory()
            printer = provider.printer
            context = provider.context

            _bitmap = withContext(Dispatchers.IO) {
                readBitmapFromFile(_item.filePath)
            }

            // Perform synchronous printing
            val result = withContext(Dispatchers.IO) {
                doPrint(_bitmap)
                handlePaperCutConfirmation()
                PrintingSuccess()
            }
            
            cleanup()
            return result

        } catch (exception: PrintingException) {
            cleanup()
            return PrintingError(exception.error)
        } catch (exception: AcquirerPrintingException) {
            cleanup()
            return PrintingError(exception.event)
        } catch (exception: Exception) {
            Log.e(tag, "Unexpected error during printing", exception)
            cleanup()
            return PrintingError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Gertec-specific abort logic
     * Currently no direct abort method in Gertec SDK
     */
    override suspend fun onAbort(item: PrintingQueueItem?): Boolean {
        cleanup()
        cleanupCoroutineScopes()
        return true
    }

    /**
     * Reads bitmap from file path or creates mock bitmap for testing
     */
    private fun readBitmapFromFile(filePath: String): Bitmap {
        try {
            // Handle mock bitmap case
            if (filePath == "mock_hello_world_bitmap") {
                return createMockHelloWorldBitmap()
            }
            
            // Handle regular file reading
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
     * Creates a mock "Hello World" bitmap for testing purposes
     * 
     * @return A bitmap containing "Hello World" text suitable for thermal printing
     */
    private fun createMockHelloWorldBitmap(): Bitmap {
        // Create bitmap with thermal printer compatible dimensions (384px width)
        val width = 384
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Draw on canvas
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        // Setup paint for main text
        val mainPaint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        // Setup paint for subtitle
        val subtitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        // Calculate text positions
        val centerX = width / 2f
        val mainTextY = (height / 2f) - ((mainPaint.descent() + mainPaint.ascent()) / 2f)
        val subtitleY = mainTextY + 60f
        
        // Draw text
        canvas.drawText("HELLO WORLD", centerX, mainTextY, mainPaint)
        canvas.drawText("Gertec Test Print", centerX, subtitleY, subtitlePaint)
        
        return bitmap
    }

    /**
     * Performs the actual printing operation using Gertec Printer
     * Returns the print request ID from the synchronous operation
     */
    private fun doPrint(bitmap: Bitmap): Int {
        try {
            // Convert to monochromatic for better printing quality
            val monochromaticBitmap = printer.printerUtils.toMonochromatic(bitmap, 0.4)
//            printer.printImageAutoResize(monochromaticBitmap)

            // Configure print settings
            val printConfig = PrintConfig()
            printConfig.width = 384  // Standard thermal printer width
            printConfig.height = 300 // Adjust height as needed
            printConfig.alignment = Alignment.CENTER

            _events.tryEmit(PrintingEvent.PRINTING)

            // Synchronous print operation - returns request ID immediately
            val printRequestId = printer.printImage(printConfig, monochromaticBitmap)
            printer.scrollPaper(2)
            
            return printRequestId

        } catch (exception: PrinterException) {
            Log.e(tag, "PrinterException during printing", exception)
            throw AcquirerPrintingException(exception.message ?: "UNKNOWN_PRINTER_ERROR")
        } catch (exception: Exception) {
            Log.e(tag, "Unexpected error during printing", exception)
            throw PrintingException(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Handles paper cut confirmation after successful printing
     * @return PrintingSuccess after handling paper cut confirmation
     */
    private suspend fun handlePaperCutConfirmation(): ProcessingResult {
        return try {
            // Request user input for paper cut confirmation
            val userResponse = requestUserInput(
                UserInputRequest.CONFIRM_PRINTER_PAPER_CUT()
            )

            val paperCutType = PaperCutType.NONE

            // Get the paper cut type from response, default to PARTIAL if null/invalid
//            val paperCutType = userResponse.value as? PaperCutType ?: PaperCutType.PARTIAL
//
//            // Perform paper cut based on user choice
//            when (paperCutType) {
//                PaperCutType.FULL -> {
//                    printer.cutPaper(CutType.PAPER_FULL_CUT)
//                }
//                PaperCutType.PARTIAL -> {
//                    printer.cutPaper(CutType.PAPER_PARTIAL_CUT)
//                }
//                PaperCutType.NONE -> {}
//            }

            when (paperCutType) {
                PaperCutType.FULL, PaperCutType.PARTIAL -> {
                    // this is to wait for the paper cut to finish
                    // it's not the best way to do it, but it works
                    // it only ever happens due to a bug in the Gertec SDK
                    // TODO: find a fix
                    kotlinx.coroutines.delay(1000)
                }
                PaperCutType.NONE -> {}
            }

            // Return success after paper cut handling
            PrintingSuccess()
            
        } catch (e: Exception) {
            Log.e(tag, "Error during paper cut confirmation: ${e.message}")
            // Still return success - paper cut failure shouldn't fail the print job
            PrintingSuccess()
        }
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
     * Cleans up resources used by the processor
     */
    private fun cleanup() {
        // Recycle original bitmap
        if (::_bitmap.isInitialized && !_bitmap.isRecycled) {
            _bitmap.recycle()
        }
    }
}
