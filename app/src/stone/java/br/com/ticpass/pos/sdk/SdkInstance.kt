package br.com.ticpass.pos.core.sdk

import android.content.Context
import android.os.Build
import android.util.Log
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
    private const val MOCK_STONE_CODE = "123456789"
    
    /**
     * Check if running on an emulator
     */
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }
    
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
     * Get the Stone Code from the initialized UserModel
     * 
     * @return The Stone Code
     * @throws IllegalStateException if the SDK is not initialized
     */
    fun getStoneCode(): String {
        val (userModel, _) = getInstance()
        return userModel.stoneCode
    }
    
    /**
     * Create a mock UserModel for debug/emulator environments
     */
    private fun createMockUserModel(): UserModel {
        val mockUser = UserModel()
        mockUser.stoneCode = MOCK_STONE_CODE
        return mockUser
    }
    
    /**
     * Create a new Stone SDK instance
     * Always initializes Stone SDK on real devices to ensure Koin is started.
     * Falls back to mock only on emulators.
     * 
     * @param context The application context
     * @return A new SDK instance
     */
    private fun createInstance(context: Context): Pair<UserModel, Context> {
        // Only use mock on emulators - real devices need Stone SDK for Koin initialization
        if (isEmulator()) {
            Log.w("SdkInstance", "Running on emulator - using mock Stone SDK")
            val mockUser = createMockUserModel()
            return Pair(mockUser, context)
        }
        
        // Initialize Stone SDK on real devices (required for Koin)
        return try {
            val stoneKeys: HashMap<StoneKeyType, String> =
                object : HashMap<StoneKeyType, String>() {
                    init {
                        put(StoneKeyType.QRCODE_AUTHORIZATION, "Bearer $STONE_QRCODE_AUTH")
                        put(StoneKeyType.QRCODE_PROVIDERID, STONE_QRCODE_PROVIDER_ID)
                    }
                }
            val userList: List<UserModel>? = StoneStart.init(context, stoneKeys)
            
            if (userList == null || userList.isEmpty()) {
                Log.w("SdkInstance", "Stone SDK returned null/empty user list - falling back to mock")
                val mockUser = createMockUserModel()
                return Pair(mockUser, context)
            }
            
            val userModel = userList.first()
            val appName = context.packageManager.getApplicationLabel(context.applicationInfo).toString()
            Stone.setAppName(appName)
            
            Pair(userModel, context)
        } catch (e: Exception) {
            Log.e("SdkInstance", "Failed to initialize Stone SDK: ${e.message} - falling back to mock", e)
            val mockUser = createMockUserModel()
            Pair(mockUser, context)
        }
    }
}
