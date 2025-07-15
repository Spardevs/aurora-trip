package br.com.ticpass.pos.sdk

import android.content.Context

/**
 * Base interface for all acquirer provider types
 * 
 * All acquirer providers must implement these common functions
 */
interface AcquirerProvider {
    /**
     * Check if the provider has been initialized
     * 
     * @return true if the provider is initialized, false otherwise
     */
    fun isInitialized(): Boolean
    
    /**
     * Initialize the provider with application context
     * 
     * @param appContext The application context
     */
    fun initialize(appContext: Context)
}
