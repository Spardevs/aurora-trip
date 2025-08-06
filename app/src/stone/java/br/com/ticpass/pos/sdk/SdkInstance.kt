package br.com.ticpass.pos.sdk

import android.content.Context
import br.com.ticpass.Constants.STONE_QRCODE_AUTH
import br.com.ticpass.Constants.STONE_QRCODE_PROVIDER_ID
import br.com.ticpass.pos.BuildConfig
import stone.application.StoneStart
import stone.user.UserModel
import stone.utils.Stone
import stone.utils.keys.StoneKeyType


/**
 * Central holder for the Stone SDK instance
 * 
 * This ensures a single shared SDK instance across all providers
 */
object SdkInstance {
    // Single shared SDK instance
    private var _instance: Pair<UserModel, Context>? = null
    private var initialized = false
    
    /**
     * Initialize the Stone SDK instance
     * 
     * @param context The application context
     * @return The initialized SDK instance
     */
    fun initialize(context: Context): Pair<UserModel, Context> {
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
    fun getInstance(): Pair<UserModel, Context> {
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
    private fun createInstance(context: Context): Pair<UserModel, Context> {
        val stoneKeys: HashMap<StoneKeyType, String> =
            object : HashMap<StoneKeyType, String>() {
                init {
                    put(StoneKeyType.QRCODE_AUTHORIZATION, "Bearer $STONE_QRCODE_AUTH")
                    put(StoneKeyType.QRCODE_PROVIDERID, STONE_QRCODE_PROVIDER_ID)
                }
            }
        val userList: List<UserModel> = StoneStart.init(context, stoneKeys)
            ?: throw IllegalStateException("Failed to initialize Stone SDK: User list is null")
        val userModel = userList.first()

        val appName = context.packageManager.getApplicationLabel(context.applicationInfo).toString()
        Stone.setAppName(appName)

        return Pair(userModel, context)
    }
}
