package br.com.ticpass.pos.sdk

import android.content.Context
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag

/**
 * Central holder for the PagSeguro SDK instance
 * 
 * This ensures a single shared SDK instance across all providers
 */
object SdkInstance {
    // Single shared SDK instance
    private var _instance: PlugPag? = null
    private var initialized = false
    
    /**
     * Initialize the PagSeguro SDK instance
     * 
     * @param context The application context
     * @return The initialized SDK instance
     */
    fun initialize(context: Context): PlugPag {
        if (!initialized) {
            _instance = createInstance(context)
            initialized = true
        }
        return _instance!!
    }
    
    /**
     * Get the PagSeguro SDK instance
     * 
     * @return The initialized SDK instance
     * @throws IllegalStateException if the SDK is not initialized
     */
    fun getInstance(): PlugPag {
        return _instance ?: throw IllegalStateException("PagSeguro SDK not initialized. Call initialize() first.")
    }
    
    /**
     * Check if the SDK is initialized
     * 
     * @return true if the SDK is initialized, false otherwise
     */
    fun isInitialized(): Boolean = initialized
    
    /**
     * Create a new PagSeguro SDK instance
     * 
     * @param context The application context
     * @return A new SDK instance
     */
    private fun createInstance(context: Context): PlugPag {
        // PagSeguro-specific SDK initialization that uses the application context
        // For example: PlugPag(context) or something similar
        // In a real implementation, you'd pass the context to the SDK initialization
        return PlugPag(context).apply {
            // Simulate using the context parameter
            // In a real implementation, you might do something like:
            // initialize(context)
        }
    }
}
