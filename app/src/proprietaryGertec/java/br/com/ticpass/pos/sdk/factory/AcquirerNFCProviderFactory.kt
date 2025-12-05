package br.com.ticpass.pos.core.sdk.factory

import android.content.Context

/**
 * Gertec NFC provider factory (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Throws UnsupportedOperationException.
 */
class GertecNFCProvider

typealias AcquirerNFCProvider = () -> GertecNFCProvider

/**
 * Factory for creating NFC provider instances (NO-OP)
 */
class AcquirerNFCProviderFactory(
    private val context: Context,
) {
    fun create(): AcquirerNFCProvider {
        return {
            throw UnsupportedOperationException(
                "NFC operations not supported in proprietary Gertec variant"
            )
        }
    }
}
