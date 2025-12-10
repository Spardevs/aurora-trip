package br.com.ticpass.pos.core.sdk.nfc

import br.com.ticpass.pos.core.sdk.AcquirerProvider

/**
 * Interface for NFC processing providers
 */
interface BaseNFCProvider<T> : AcquirerProvider {
    /**
     * Get the flavor-specific implementation instance
     *
     * @return The concrete implementation of type T
     */
    fun getInstance(): T
}
