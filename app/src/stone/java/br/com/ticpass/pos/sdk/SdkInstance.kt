package br.com.ticpass.pos.sdk

import android.content.Context
import stone.user.UserModel

/**
 * Central holder for the Stone SDK instance
 * 
 * This ensures a single shared SDK instance across all providers
 */
object SdkInstance {
    // Single shared SDK instance
    private var _instance: UserModel? = null
    private var initialized = false
    
    /**
     * Initialize the Stone SDK instance
     * 
     * @param context The application context
     * @return The initialized SDK instance
     */
    fun initialize(context: Context): UserModel {
        if (!initialized) {
            _instance = createInstance(context)
            initialized = true
        }
        return _instance!!
    }
    
    /**
     * Get the Stone SDK instance
     * 
     * @return The initialized SDK instance
     * @throws IllegalStateException if the SDK is not initialized
     */
    fun getInstance(): UserModel {
        return _instance ?: throw IllegalStateException("Stone SDK not initialized. Call initialize() first.")
    }
    
    /**
     * Check if the SDK is initialized
     * 
     * @return true if the SDK is initialized, false otherwise
     */
    fun isInitialized(): Boolean = initialized
    
    /**
     * Create a new Stone SDK instance
     * 
     * @param context The application context
     * @return A new SDK instance
     */
    private fun createInstance(context: Context): UserModel {
        // Stone-specific SDK initialization that uses the application context
        // For example: UserModel(context) or something similar
        // In a real implementation, you'd pass the context to the SDK initialization
        return UserModel().apply { 
            // Simulate using the context parameter
            // In a real implementation, you might do something like:
            // initialize(context)
        }
    }
}
