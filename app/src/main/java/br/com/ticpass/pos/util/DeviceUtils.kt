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

/**
 * Utility class for device-related operations
 */
object DeviceUtils {

    /**
     * Gets the device serial number with compatibility for API level 26+
     *
     * For API 26+: Uses Build.getSerial() with proper permission handling
     * For API <26: Uses Build.SERIAL
     *
     * Falls back to Settings.Secure.ANDROID_ID if serial cannot be obtained
     *
     * @param context Context needed for permission check
     * @return Device serial number
     */
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    @Suppress("DEPRECATION")
    fun getDeviceSerial(context: Context): String {
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
     * @return Device model (e.g., "SM-G973F", "Pixel 5")
     */
    fun getDeviceModel(): String = Build.MODEL

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