package br.com.ticpass.pos.sdk.refund

import android.content.Context
import br.com.ticpass.pos.sdk.SdkInstance
import br.com.ticpass.pos.sdk.factory.AcquirerRefundProvider
import br.com.ticpass.pos.sdk.factory.RefundProviderFactory
import br.com.ticpass.pos.sdk.payment.PaymentProvider

/**
 * Stone-specific implementation of RefundProvider
 * This file overrides the base implementation by providing a Stone-specific provider
 */
object RefundProvider : BaseRefundProvider<AcquirerRefundProvider> {
    private var initialized = false
    
    override fun isInitialized(): Boolean = initialized
    
    override fun initialize(appContext: Context) {
        if (!initialized) {
            // Use the shared SDK instance
            SdkInstance.initialize(appContext)
            initialized = true
        }
    }
    
    override fun getInstance(): AcquirerRefundProvider {
        if (!PaymentProvider.isInitialized()) {
            throw IllegalStateException("Payment provider not initialized. Call initialize() first.")
        }
        val (_, context) =  SdkInstance.getInstance()
        val acquirerRefundFactory = RefundProviderFactory(context)
        return acquirerRefundFactory.create()
    }
}