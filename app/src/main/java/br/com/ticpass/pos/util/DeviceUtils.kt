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
}