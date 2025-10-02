package br.com.ticpass.pos.sdk.factory

import android.content.Context

/**
 * Gertec SK210 Refund provider factory (NO-OP)
 * 
 * This variant does not support refund operations.
 * Throws UnsupportedOperationException.
 */
class GertecSk210RefundProvider

typealias RefundProvider = (String) -> GertecSk210RefundProvider

/**
 * Factory for creating Refund provider instances (NO-OP)
 */
class RefundProviderFactory(
    private val context: Context,
) {
    fun create(): RefundProvider {
        return { atk ->
            throw UnsupportedOperationException(
                "Refund operations not supported in proprietary Gertec SK210 variant"
            )
        }
    }
}
