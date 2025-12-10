package br.com.ticpass.pos.core.sdk.printing

import br.com.ticpass.pos.core.sdk.AcquirerProvider

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
