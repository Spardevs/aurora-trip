package br.com.ticpass.pos.sdk.printing

import android.content.Context
import br.com.ticpass.pos.sdk.SdkInstance
import br.com.ticpass.pos.sdk.factory.AcquirerPrintingProvider
import br.com.ticpass.pos.sdk.factory.AcquirerPrintingProviderFactory
import br.com.ticpass.pos.sdk.payment.PaymentProvider

/**
 * Stone-specific implementation of PrintingProvider
 * This file overrides the base implementation by providing a Stone-specific provider
 */
object PrintingProvider : BasePrintingProvider<AcquirerPrintingProvider> {
    private var initialized = false
    
    override fun isInitialized(): Boolean = initialized
    
    override fun initialize(appContext: Context) {
        if (!initialized) {
            // Use the shared SDK instance
            SdkInstance.initialize(appContext)
            initialized = true
        }
    }
    
    override fun getInstance(): AcquirerPrintingProvider {
        if (!PaymentProvider.isInitialized()) {
            throw IllegalStateException("Payment provider not initialized. Call initialize() first.")
        }
        val (_, context) =  SdkInstance.getInstance()
        val acquirerPrintingFactory = AcquirerPrintingProviderFactory(context)
        return acquirerPrintingFactory.create()
    }
}