package br.com.ticpass.pos.sdk.payment

/**
 * PagSeguro-specific implementation of PaymentProvider
 * This file overrides the base implementation by providing a PagSeguro-specific provider
 */
object PaymentProvider {
    /**
     * The singleton instance for PagSeguro payment provider
     */
    val instance: BasePaymentProvider by lazy {
        // Create and return the PagSeguro implementation
        // initialize plugpag
        throw NotImplementedError("PagSeguro payment provider is not implemented yet")
    }
}
