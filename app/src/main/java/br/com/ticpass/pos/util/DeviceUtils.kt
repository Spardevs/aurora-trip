package br.com.ticpass.pos.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.*
import android.provider.Settings.Secure.*
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import br.com.ticpass.pos.BuildConfig
import br.com.ticpass.pos.sdk.AcquirerSdk

/**
 * Utility class for device-related operations
 */
object DeviceUtils {
    // Mock values from BuildConfig (configured via local.properties)
    private val mockDeviceModel: String
        get() = BuildConfig.MOCK_DEVICE_MODEL
    
    private val mockDeviceSerial: String
        get() = BuildConfig.MOCK_DEVICE_SERIAL

    /**
     * Gets the device serial number with compatibility for API level 26+
     *
     * Priority order:
     * 1. Acquirer SDK serial (PagSeguro PlugPag.getSerialNumber())
     * 2. Build.getSerial() (API 26+) or Build.SERIAL (API <26)
     * 3. Settings.Secure.ANDROID_ID as fallback
     *
     * In debug builds, returns a mocked serial number
     *
     * @param context Context needed for permission check
     * @return Device serial number (or mock in debug mode)
     */
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    @Suppress("DEPRECATION")
    fun getDeviceSerial(context: Context): String {
        // Return mock in debug mode (from local.properties)
        if (BuildConfig.DEBUG) {
            return mockDeviceSerial
        }
        
        // Try to get serial from Acquirer SDK first (more reliable on POS devices)
        try {
            if (AcquirerSdk.isInitialized()) {
                val sdkSerial = AcquirerSdk.getDeviceSerial()
                if (!sdkSerial.isNullOrBlank()) {
                    return sdkSerial
                }
            }
        } catch (e: Exception) {
            // SDK not available or failed, continue to fallback methods
        }
        
        // Fallback to Android Build serial
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For API 26+
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED) {
                    Build.getSerial()
                } else {
                    // Permission not granted, fallback to Android ID
                    getString(context.contentResolver, ANDROID_ID) ?: "unknown"
                }
            } else {
                // For API < 26
                Build.SERIAL
            }
        } catch (e: SecurityException) {
            // Handle security exception by falling back to Android ID
            getString(context.contentResolver, ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            // For any other unexpected issues, fall back to Android ID or just "unknown"
            try {
                getString(context.contentResolver, ANDROID_ID) ?: "unknown"
            } catch (e: Exception) {
                "unknown"
            }
        }
    }

    /**
     * Gets the device model name
     * In debug builds, returns a mocked model name from local.properties
     * Removes non-alphanumeric characters and converts to lowercase
     * @return Device model (e.g., "smg973f", "pixel5") or mock in debug mode
     */
    fun getDeviceModel(): String {
        return if (BuildConfig.DEBUG) {
            mockDeviceModel
        } else {
            Build.MODEL.lowercase().filter { it.isLetterOrDigit() }
        }
    }

    /**
     * Gets the device manufacturer name
     * @return Manufacturer name (e.g., "Samsung", "Google")
     */
    fun getDeviceManufacturer(): String = Build.MANUFACTURER

    /**
     * Gets the device brand name
     * @return Brand name (e.g., "samsung", "google")
     */
    fun getDeviceBrand(): String = Build.BRAND

    /**
     * Gets the device product name
     * @return Product name (e.g., "beyond1lte", "redfin")
     */
    fun getDeviceProduct(): String = Build.PRODUCT

    /**
     * Gets a formatted device name combining manufacturer and model
     * @return Formatted device name (e.g., "Samsung SM-G973F", "Google Pixel 5")
     */
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }
        val model = Build.MODEL
        
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Gets comprehensive device information as a map
     * @return Map containing all device information
     */
    fun getDeviceInfo(): Map<String, String> = mapOf(
        "manufacturer" to Build.MANUFACTURER,
        "brand" to Build.BRAND,
        "model" to Build.MODEL,
        "product" to Build.PRODUCT,
        "device" to Build.DEVICE,
        "hardware" to Build.HARDWARE,
        "board" to Build.BOARD,
        "display" to Build.DISPLAY,
        "name" to getDeviceName()
    )

    /**
     * Gets the current acquirer based on the build flavor
     * @return Acquirer name: "stone", "pagseguro", or "proprietary"
     */
    fun getAcquirer(): String {
        return when (BuildConfig.FLAVOR) {
            "proprietaryGertec" -> "proprietary"
            "pagseguro" -> "pagseguro"
            "stone" -> "stone"
            else -> "pagseguro"
        }
    }

    /**
     * Gets the raw build flavor name
     * @return Build flavor name (e.g., "proprietaryGertec", "pagseguro", "stone")
     */
    fun getBuildFlavor(): String = BuildConfig.FLAVOR

    /**
     * Checks if the current build is for a specific acquirer
     * @param acquirer The acquirer name to check ("stone", "pagseguro", or "proprietary")
     * @return True if the current build matches the specified acquirer
     */
    fun isAcquirer(acquirer: String): Boolean {
        return getAcquirer().equals(acquirer, ignoreCase = true)
    }
}