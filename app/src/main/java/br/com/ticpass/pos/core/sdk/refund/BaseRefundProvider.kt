package br.com.ticpass.pos.core.sdk.refund

import br.com.ticpass.pos.core.sdk.AcquirerProvider

/**
 * Interface for refund providers
 */
interface BaseRefundProvider<T> : AcquirerProvider {

    /**
     * Get the flavor-specific implementation instance
     *
     * @return The concrete implementation of type T
     */
    fun getInstance(): T
}
