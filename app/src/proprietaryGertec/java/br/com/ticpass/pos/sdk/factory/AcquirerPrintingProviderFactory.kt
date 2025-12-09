package br.com.ticpass.pos.core.sdk.factory

import android.content.Context
import android.util.Log
import br.com.gertec.easylayer.printer.Printer
import br.com.gertec.easylayer.printer.PrinterError
import java.util.Locale

/**
 * Gertec printing provider
 * This wraps the Gertec SDK printing functionality
 * Following the pattern from Gertec example:
 * - Printer.getInstance(context, listener) is called with specific listener
 * - PrinterUtils is obtained from printer
 */
class GertecPrintingProvider(
    val context: Context,
    val printer: Printer,
)

typealias AcquirerPrintingProvider = () -> GertecPrintingProvider

/**
 * Factory for creating GertecPrintingProvider instances
 * 
 * This implements a higher-order function that accepts a listener per invocation.
 * Each call creates a Printer with its own unique callback.
 */
class AcquirerPrintingProviderFactory(
    private val context: Context,
) {
    val dummyListener : Printer.Listener = object : Printer.Listener {
        override fun onPrinterError(printerError: PrinterError) {
            val message = String.format(
                Locale.US,
                "Id: [%d] | Cause: [\"%s\"]",
                printerError.requestId,
                printerError.cause
            )
            Log.d("AcquirerPrintingProcessor", "[onPrinterError] $message")
        }

        override fun onPrinterSuccessful(printerRequestId: Int) {
            val message = String.format(Locale.US, "Id: [%d]", printerRequestId)
            Log.d("AcquirerPrintingProcessor", "[onPrinterSuccessful] $message")
        }
    }
    /**
     * Creates a function that returns a GertecPrintingProvider with a unique listener
     * Following Gertec's onCreate pattern:
     * 1. Printer.getInstance(context) - with caller-provided listener
     * 2. printer.getPrinterUtils()
     * 
     * @return A function that accepts a Printer.Listener and creates a provider with unique callbacks
     */
    fun create(): AcquirerPrintingProvider {
        return {
            val printer = Printer.getInstance(context, dummyListener)

            GertecPrintingProvider(context, printer)
        }
    }
}
