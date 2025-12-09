package br.com.ticpass.pos.core.sdk.factory

import android.content.Context
import br.com.stone.posandroid.providers.PosPrintProvider

typealias AcquirerPrintingProvider = () -> PosPrintProvider

/**
 * Factory for creating PosPrintProvider instances
 * 
 * This implements a higher-order function.
 * Initialize the factory with context, then use it with only context.
 */
class AcquirerPrintingProviderFactory(
    private val context: Context,
) {
    /**
     * Creates a function that takes only a context and returns a PosPrintProvider
     * This follows the higher-order function pattern from functional programming
     * 
     * @return A function that creates a PosPrintProvider with the configured contex
     */
    fun create(): AcquirerPrintingProvider {
        return { PosPrintProvider(context) }
    }
}
