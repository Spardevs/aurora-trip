package br.com.ticpass.pos.sdk.factory

import android.content.Context
import br.com.gertec.easylayer.printer.Printer
import br.com.gertec.easylayer.printer.PrinterUtils

/**
 * Gertec SK210 printing provider
 * This wraps the Gertec SK210 SDK printing functionality
 * Following the pattern from Gertec example:
 * - Printer.getInstance(context, listener) is called with specific listener
 * - PrinterUtils is obtained from printer
 */
class GertecSk210PrintingProvider(
    val context: Context,
    val printer: Printer,
    val printerUtils: PrinterUtils
)

typealias AcquirerPrintingProvider = (Printer.Listener) -> GertecSk210PrintingProvider

/**
 * Factory for creating GertecSk210PrintingProvider instances
 * 
 * This implements a higher-order function that accepts a listener per invocation.
 * Each call creates a Printer with its own unique callback.
 */
class AcquirerPrintingProviderFactory(
    private val context: Context,
) {
    /**
     * Creates a function that returns a GertecSk210PrintingProvider with a unique listener
     * Following Gertec's onCreate pattern:
     * 1. Printer.getInstance(context, listener) - with caller-provided listener
     * 2. printer.getPrinterUtils()
     * 
     * @return A function that accepts a Printer.Listener and creates a provider with unique callbacks
     */
    fun create(): AcquirerPrintingProvider {
        return { listener ->
            // Following Gertec example pattern with unique listener per invocation
            val printer = Printer.getInstance(context, listener)
            val printerUtils = printer.printerUtils
            
            GertecSk210PrintingProvider(context, printer, printerUtils)
        }
    }
}
