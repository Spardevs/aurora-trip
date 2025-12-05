package br.com.ticpass.pos.sdk.printing

import br.com.ticpass.pos.sdk.AcquirerProvider

/**
 * Interface for printing providers
 */
interface BasePrintingProvider<T> : AcquirerProvider {

    /**
     * Get the flavor-specific implementation instance
     *
     * @return The concrete implementation of type T
     */
    fun getInstance(): T
}
