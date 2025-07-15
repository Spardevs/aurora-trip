package br.com.ticpass.pos.sdk.nfc

import br.com.ticpass.pos.sdk.AcquirerProvider

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
