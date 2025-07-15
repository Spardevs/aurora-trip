package br.com.ticpass.pos.sdk.printing

import br.com.ticpass.pos.sdk.AcquirerProvider

/**
 * Interface for receipt printing providers
 */
interface BasePrintingProvider : AcquirerProvider {
    /**
     * Initialize the printer with a specific model
     * 
     * @param printerModel The printer model identifier
     */
    fun initialize(printerModel: String)
}
