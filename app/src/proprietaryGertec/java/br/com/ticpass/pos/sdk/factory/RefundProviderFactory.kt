package br.com.ticpass.pos.core.sdk.factory

import android.content.Context

/**
 * Gertec Refund provider factory (NO-OP)
 * 
 * This variant does not support refund operations.
 * Throws UnsupportedOperationException.
 */
class GertecRefundProvider

typealias RefundProvider = (String) -> GertecRefundProvider

/**
 * Factory for creating Refund provider instances (NO-OP)
 */
class RefundProviderFactory(
    private val context: Context,
) {
    fun create(): RefundProvider {
        return { atk ->
            throw UnsupportedOperationException(
                "Refund operations not supported in proprietary Gertec variant"
            )
        }
    }
}
