package br.com.ticpass.pos.data.room.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class GPSService(
    private val context: Context,
    private val callback: (latitude: Double, longitude: Double) -> Unit
) {

    private val locationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    private val handlerThread = HandlerThread("GPSServiceThread").apply { start() }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val latitude = location.latitude
            val longitude = location.longitude
            callback(latitude, longitude)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun stop() {
        locationManager?.removeUpdates(locationListener)
        handlerThread.quit()
    }

    fun getUserCoordinates(listener: LocationListener) {
        try {
            if (locationManager == null) {
                Log.e("GPSService", "LocationManager not available.")
                // Fallback or notify via listener that the LocationManager is not available
                listener.onLocationChanged(Location("").apply {
                    // Optionally set default or error values for latitude and longitude
                })
                return
            }

            val handler = Handler(handlerThread.looper)
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPermission) {
                handler.post {
                    locationManager?.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                listener.onLocationChanged(location)
                                this@GPSService.stop() // Consider if you want to stop the service here
                            }

                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        },
                        null
                    )
                }
            } else {
                Log.e("GPSService", "Location permission not granted.")
                // Fallback or notify via listener that permissions are not granted
                listener.onLocationChanged(Location("").apply {
                    // Optionally set default or error values for latitude and longitude
                })
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } catch (e: Exception) {
            Log.e("GPSService", "Error requesting location updates: ${e.message}")
            // Optionally notify the external listener about the error
            listener.onLocationChanged(Location("").apply {
                // Optionally set default or error values for latitude and longitude due to error
            })
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
