package br.com.ticpass.pos.core.sdk.printing

import android.content.Context
import br.com.ticpass.pos.core.sdk.SdkInstance
import br.com.ticpass.pos.core.sdk.factory.AcquirerPrintingProvider
import br.com.ticpass.pos.core.sdk.factory.AcquirerPrintingProviderFactory

/**
 * Gertec-specific implementation of PrintingProvider
 * This file overrides the base implementation by providing a Gertec-specific provider
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
        if (!isInitialized()) {
            throw IllegalStateException("Printing provider not initialized. Call initialize() first.")
        }
        val context = SdkInstance.getInstance()
        val acquirerPrintingFactory = AcquirerPrintingProviderFactory(context)
        return acquirerPrintingFactory.create()
    }
}
