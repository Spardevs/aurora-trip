package br.com.ticpass.pos.sdk.factory

import android.content.Context
import br.com.stone.posandroid.providers.PosMifareProvider

typealias AcquirerNFCProvider = () -> PosMifareProvider

/**
 * Factory for creating PosMifareProvider instances
 * 
 * This implements a higher-order function.
 * Initialize the factory with context, then use it with only context.
 */
class AcquirerNFCProviderFactory(
    private val context: Context,
) {
    /**
     * Creates a function that takes only a context and returns a PosMifareProvider
     * This follows the higher-order function pattern from functional programming
     * 
     * @return A function that creates a PosMifareProvider with the configured contex
     */
    fun create(): AcquirerNFCProvider {
        return { PosMifareProvider(context) }
    }
}
