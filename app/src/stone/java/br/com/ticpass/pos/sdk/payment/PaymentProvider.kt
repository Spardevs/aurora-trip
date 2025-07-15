package br.com.ticpass.pos.sdk.payment

/**
 * Stone-specific implementation of PaymentProvider
 * This file overrides the base implementation by providing a Stone-specific provider
 */
object PaymentProvider {
    /**
     * The singleton instance for Stone payment provider
     */
    val instance: BasePaymentProvider by lazy {
        // Create and return the Stone implementation
        // initialize Stone SDK
        throw NotImplementedError("Stone payment provider is not implemented yet")
    }
}
