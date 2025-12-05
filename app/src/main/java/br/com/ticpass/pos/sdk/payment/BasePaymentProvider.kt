package br.com.ticpass.pos.sdk.payment

import br.com.ticpass.pos.sdk.AcquirerProvider

/**
 * Interface for payment processing providers
 */
interface BasePaymentProvider<T> : AcquirerProvider {

    /**
     * Get the flavor-specific implementation instance
     *
     * @return The concrete implementation of type T
     */
    fun getInstance(): T
}
