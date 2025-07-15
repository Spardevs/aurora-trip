package br.com.ticpass.pos.sdk.nfc

import br.com.ticpass.pos.sdk.AcquirerProvider

/**
 * Interface for NFC processing providers
 */
interface BaseNFCProvider : AcquirerProvider {
    /**
     * Initialize the NFC provider
     */
    fun initialize()
}
