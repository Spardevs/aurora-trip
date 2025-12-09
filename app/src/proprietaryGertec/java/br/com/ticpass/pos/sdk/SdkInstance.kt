package br.com.ticpass.pos.core.sdk

import android.content.Context

/**
 * Central holder for the Gertec SDK instance
 * 
 * This ensures a single shared SDK instance across all providers
 */
object SdkInstance {
    // Single shared SDK instance
    private var _instance: Context? = null
    private var initialized = false
    
    /**
     * Initialize the Gertec SDK instance
     * 
     * @param context The application context
     * @return The initialized SDK instance (context)
     */
    fun initialize(context: Context): Context {
        if (!initialized) {
            _instance = context.applicationContext
            initialized = true
        }
        return _instance!!
    }
    
    /**
     * Get the Gertec SDK instance
     * 
     * @return The initialized SDK instance (context)
     * @throws IllegalStateException if the SDK is not initialized
     */
    fun getInstance(): Context {
        return _instance ?: throw IllegalStateException("Gertec SDK not initialized. Call initialize() first.")
    }
    
    /**
     * Check if the SDK is initialized
     * 
     * @return true if the SDK is initialized, false otherwise
     */
    fun isInitialized(): Boolean = initialized
}
