package br.com.ticpass.pos.sdk.factory

import android.content.Context

/**
 * Gertec SK210 NFC provider factory (NO-OP)
 * 
 * This variant does not support NFC operations.
 * Throws UnsupportedOperationException.
 */
class GertecSk210NFCProvider

typealias AcquirerNFCProvider = () -> GertecSk210NFCProvider

/**
 * Factory for creating NFC provider instances (NO-OP)
 */
class AcquirerNFCProviderFactory(
    private val context: Context,
) {
    fun create(): AcquirerNFCProvider {
        return {
            throw UnsupportedOperationException(
                "NFC operations not supported in proprietary Gertec SK210 variant"
            )
        }
    }
}
