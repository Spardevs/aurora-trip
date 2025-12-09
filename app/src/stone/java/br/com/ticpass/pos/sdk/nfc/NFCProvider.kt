package br.com.ticpass.pos.core.sdk.nfc

import android.content.Context
import br.com.ticpass.pos.core.sdk.SdkInstance
import br.com.ticpass.pos.core.sdk.factory.AcquirerNFCProvider
import br.com.ticpass.pos.core.sdk.factory.AcquirerNFCProviderFactory

/**
 * Stone-specific implementation of NFCProvider
 * This file overrides the base implementation by providing a Stone-specific provider
 */
object NFCProvider : BaseNFCProvider<AcquirerNFCProvider> {
    private var initialized = false
    
    override fun isInitialized(): Boolean = initialized
    
    override fun initialize(appContext: Context) {
        if (!initialized) {
            // Use the shared SDK instance
            SdkInstance.initialize(appContext)
            initialized = true
        }
    }
    
    override fun getInstance(): AcquirerNFCProvider {
        if (!isInitialized()) {
            throw IllegalStateException("NFC provider not initialized. Call initialize() first.")
        }
        val (_, context) =  SdkInstance.getInstance()
        val acquirerNFCFactory = AcquirerNFCProviderFactory(context)
        return acquirerNFCFactory.create()
    }
}