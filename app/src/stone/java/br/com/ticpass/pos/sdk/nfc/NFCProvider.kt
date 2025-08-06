package br.com.ticpass.pos.sdk.nfc

import android.content.Context
import br.com.ticpass.pos.sdk.SdkInstance
import stone.user.UserModel

/**
 * Stone-specific implementation of NFCProvider
 * This file overrides the base implementation by providing a Stone-specific provider
 */
object NFCProvider : BaseNFCProvider<UserModel> {
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
            throw IllegalStateException("NFC provider not initialized. Call initialize() first.")
        }
        val (userModel) =  SdkInstance.getInstance()
        return userModel
    }
}