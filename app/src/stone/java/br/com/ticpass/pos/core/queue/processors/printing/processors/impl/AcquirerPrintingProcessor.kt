package br.com.ticpass.pos.core.queue.processors.printing.processors.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import br.com.stone.posandroid.providers.PosPrintProvider
import br.com.ticpass.pos.core.queue.error.ProcessingErrorEvent
import br.com.ticpass.pos.core.queue.models.PrintingError
import br.com.ticpass.pos.core.queue.models.PrintingSuccess
import br.com.ticpass.pos.core.queue.models.ProcessingResult
import br.com.ticpass.pos.core.queue.processors.printing.AcquirerPrintingException
import br.com.ticpass.pos.core.queue.processors.printing.exceptions.PrintingException
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingEvent
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingQueueItem
import br.com.ticpass.pos.core.queue.processors.printing.processors.core.PrintingProcessorBase
import br.com.ticpass.pos.core.sdk.AcquirerSdk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stone.application.enums.Action
import stone.application.enums.ErrorsEnum
import stone.application.interfaces.StoneActionCallback
import androidx.core.graphics.scale
import javax.inject.Inject

/**
 * Stone Printing Processor
 * Processes printing using the Stone SDK via direct SDK access.
 */
class AcquirerPrintingProcessor @Inject constructor() : PrintingProcessorBase() {

    private val tag = this.javaClass.simpleName
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var _item: PrintingQueueItem
    private lateinit var printingProvider: PosPrintProvider
    private lateinit var _bitmap: Bitmap

    override suspend fun processPrinting(item: PrintingQueueItem): ProcessingResult {
        try {
            _item = item
            val printingProviderFactory = AcquirerSdk.printing.getInstance()
            printingProvider = printingProviderFactory()

            _bitmap = withContext(Dispatchers.IO) {
                val raw = readBitmapFromFile(_item.filePath)
                val scaled = scaleBitmap(raw, 384)

                raw.recycle()

                scaled
            }

            val result = withContext(Dispatchers.IO) {
                doPrint(_bitmap)
            }

            cleanup()

            return result
        }
        catch (exception: PrintingException) {
            return PrintingError(exception.error)
        }
        catch (exception: AcquirerPrintingException) {
            return PrintingError(exception.event)
        }
        catch (exception: Exception) {
            return PrintingError(ProcessingErrorEvent.GENERIC)
        }
    }

    /**
     * Stone-specific abort logic
     * Cancels any ongoing printing transaction
     */
    override suspend fun onAbort(item: PrintingQueueItem?): Boolean {
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
     * future printing operations.
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
        if(::_bitmap.isInitialized) _bitmap.recycle()
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
     * Scales the given bitmap to fit within the specified max width while maintaining aspect ratio.
     * It's important to ensure that the image fits properly on the receipt paper.
     * This process is not handled by the Stone SDK, so we need to do it manually.
     *
     * @param bitmap The original bitmap to be scaled
     * @param maxWidth The maximum width for the scaled bitmap
     * @return A new Bitmap object that is scaled to fit within the specified width
     */
    private suspend fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        return withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height

            val widthRatio = maxWidth.toFloat() / width.toFloat()
            val heightRatio = bitmap.height * widthRatio
            val scaleRatio = widthRatio.coerceAtMost(heightRatio)

            val newWidth = (width * scaleRatio).toInt()
            val newHeight = (height * scaleRatio).toInt()

            bitmap.scale(newWidth, newHeight)
        }
    }

    /**
     * Performs the actual printing operation using the PosPrintProvider.
     */
    private suspend fun doPrint(bitmap: Bitmap): ProcessingResult {
        val deferred = CompletableDeferred<ProcessingResult>()

        try {
            printingProvider.addBitmap(bitmap)

            printingProvider.setConnectionCallback(
                object : StoneActionCallback {
                    override fun onStatusChanged(action: Action?) {
                        // this callback is actually never called by the SDK during printing
                        _events.tryEmit(PrintingEvent.PRINTING)
                    }

                    override fun onSuccess() {
                        deferred.complete(
                            PrintingSuccess()
                        )
                    }

                    override fun onError() {
                        val error = printingProvider.listOfErrors?.last() ?: ErrorsEnum.UNKNOWN_ERROR
                        val exception = AcquirerPrintingException(error)
                        deferred.complete(
                            PrintingError(exception.event)
                        )
                    }
                }
            )

            _events.tryEmit(PrintingEvent.PRINTING)

            printingProvider.execute()
        }
        catch (exception: Exception) {
            deferred.complete(
                PrintingError(ProcessingErrorEvent.GENERIC)
            )
        }

        return deferred.await()
    }
}