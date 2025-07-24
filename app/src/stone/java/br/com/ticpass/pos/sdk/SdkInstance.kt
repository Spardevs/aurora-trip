package br.com.ticpass.pos.sdk

import android.content.Context
import br.com.ticpass.pos.BuildConfig
import stone.application.StoneStart
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
        val isDebug = BuildConfig.DEBUG
        if (isDebug) return UserModel()

        val userList: List<UserModel> = StoneStart.init(context)
            ?: throw IllegalStateException("Failed to initialize Stone SDK: User list is null")
        return userList.first()
    }
}
