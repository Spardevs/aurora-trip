package br.com.ticpass.pos.sdk.printing

import android.content.Context
import br.com.ticpass.pos.sdk.SdkInstance
import stone.user.UserModel

/**
 * Stone-specific implementation of PrintingProvider
 * This file overrides the base implementation by providing a Stone-specific provider
 */
object PrintingProvider : BasePrintingProvider<UserModel> {
    private var initialized = false
    
    override fun isInitialized(): Boolean = initialized
    
    override fun initialize(appContext: Context) {
        if (!initialized) {
            // Use the shared SDK instance
            SdkInstance.initialize(appContext)
            initialized = true
        }
    }
    
    override fun getInstance(): UserModel {
        if (!isInitialized()) {
            throw IllegalStateException("Printing provider not initialized. Call initialize() first.")
        }
        return SdkInstance.getInstance()
    }
}