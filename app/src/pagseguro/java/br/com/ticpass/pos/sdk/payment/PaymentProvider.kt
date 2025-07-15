package br.com.ticpass.pos.sdk.payment

import android.content.Context
import br.com.ticpass.pos.sdk.SdkInstance
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag

/**
 * PagSeguro-specific implementation of PaymentProvider
 * This file overrides the base implementation by providing a PagSeguro-specific provider
 */
object PaymentProvider : BasePaymentProvider<PlugPag> {
    private var initialized = false
    
    override fun isInitialized(): Boolean = initialized
    
    override fun initialize(appContext: Context) {
        if (!initialized) {
            // Use the shared SDK instance
            SdkInstance.initialize(appContext)
            initialized = true
        }
    }
    
    override fun getInstance(): PlugPag {
        if (!isInitialized()) {
            throw IllegalStateException("Payment provider not initialized. Call initialize() first.")
        }
        return SdkInstance.getInstance()
    }
}