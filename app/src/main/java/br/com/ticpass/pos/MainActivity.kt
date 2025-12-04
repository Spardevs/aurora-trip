/*
 * Copyright (c) 2025 Ticpass. All rights reserved.
 *
 * PROPRIETARY AND CONFIDENTIAL
 *
 * This software is the confidential and proprietary information of Ticpass
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Ticpass.
 *
 * Unauthorized copying, distribution, or use of this software, via any medium,
 * is strictly prohibited without the express written permission of Ticpass.
 */

package br.com.ticpass.pos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import br.com.ticpass.pos.core.util.ConnectionStatusBar
import br.com.ticpass.pos.core.util.ConnectivityMonitor
import br.com.ticpass.pos.core.util.DeviceUtils
import br.com.ticpass.pos.core.util.SessionPrefsManagerUtils
import br.com.ticpass.pos.domain.user.repository.UserRepository
import br.com.ticpass.pos.presentation.login.activities.LoginActivity
import br.com.ticpass.pos.presentation.login.activities.LoginPermissionsActivity
import br.com.ticpass.pos.presentation.shared.activities.BaseActivity
import com.topjohnwu.superuser.internal.UiThreadHandler.handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import br.com.ticpass.pos.data.device.remote.service.DeviceService
import br.com.ticpass.pos.data.device.remote.dto.RegisterDeviceRequest
import br.com.ticpass.pos.data.user.local.dao.UserDao
import br.com.ticpass.pos.presentation.login.activities.LoadingDownloadFragmentActivity
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private var connectivityMonitor: ConnectivityMonitor? = null
    private var connectionStatusBar: ConnectionStatusBar? = null

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var deviceService: DeviceService

    @Inject
    lateinit var userDao: UserDao

    companion object {
        lateinit var location: Location
        lateinit var appContext: Context
        @SuppressLint("StaticFieldLeak")
        lateinit var activity: Activity
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun hasAllPermissions(): Boolean =
        REQUIRED_PERMISSIONS.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) ==
                    PackageManager.PERMISSION_GRANTED
        }

    /**
     * Logs comprehensive device information including model, acquirer, serial, and stone code
     */
    private fun logDeviceInfo(): Map<String, String> {
        return try {
            val model = DeviceUtils.getDeviceModel()
            val acquirer = DeviceUtils.getAcquirer()
            val serial = DeviceUtils.getDeviceSerial(this)

            Timber.tag("DeviceInfo").i("════")
            Timber.tag("DeviceInfo").i("    DEVICE INFORMATION")
            Timber.tag("DeviceInfo").i("════")
            Timber.tag("DeviceInfo").i("Model:    $model")
            Timber.tag("DeviceInfo").i("Acquirer:    $acquirer")
            Timber.tag("DeviceInfo").i("Serial:    $serial")
            Timber.tag("DeviceInfo").i("════")

            mapOf(
                "model" to model,
                "acquirer" to acquirer,
                "serial" to serial
            )
        } catch (e: Exception) {
            Timber.tag("DeviceInfo").e(e, "Error logging device info: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Registers the device with the backend API -- suspend function so caller can await completion
     */
    private suspend fun registerDevice(deviceInfo: Map<String, String>) {
        try {
            val serial = deviceInfo["serial"] ?: run {
                Timber.tag("DeviceRegistration").w("Missing serial, skipping registration")
                return
            }
            val acquirer = deviceInfo["acquirer"] ?: run {
                Timber.tag("DeviceRegistration").w("Missing acquirer, skipping registration")
                return
            }
            val variant = deviceInfo["model"] ?: run {
                Timber.tag("DeviceRegistration").w("Missing variant, skipping registration")
                return
            }

            val request = RegisterDeviceRequest(
                serial = serial,
                acquirer = acquirer,
                variant = variant
            )

            Timber.tag("DeviceRegistration").d("Registering device: $request")

            val response = deviceService.registerDevice(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                Timber.tag("DeviceRegistration").i("Device registered successfully: $responseBody")

                // Save the device ID returned from the API instead of the serial
                responseBody?.id?.let { deviceId ->
                    SessionPrefsManagerUtils.saveDeviceId(deviceId)
                    Timber.tag("DeviceRegistration").i("Device ID saved: $deviceId")
                }
            } else {
                Timber.tag("DeviceRegistration").w(
                    "Failed to register device. Code: ${response.code()}, Error: ${
                        response.errorBody()?.string()
                    }"
                )
            }
        } catch (e: HttpException) {
            Timber.tag("DeviceRegistration")
                .e(e, "HTTP error during device registration: ${e.message()}")
        } catch (e: IOException) {
            Timber.tag("DeviceRegistration").e(e, "Network error during device registration: ${e.message}")
        } catch (e: Exception) {
            Timber.tag("DeviceRegistration").e(e, "Unexpected error during device registration: ${e.message}")
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = applicationContext
        activity = this

        // Log device information
        val deviceInfo = logDeviceInfo()

        // Inicializa UI componentes que não dependem da navegação imediata
        connectionStatusBar = ConnectionStatusBar(this)

        lifecycleScope.launch {
            try {
                // limite de tempo para não pendurar indefinidamente (ajuste conforme necessário)
                withTimeout(5_000L) {
                    registerDevice(deviceInfo)
                }
            } catch (t: TimeoutCancellationException) {
                Timber.tag("DeviceRegistration").w("Device registration timed out after 5s")
            } catch (e: Exception) {
                Timber.tag("DeviceRegistration").e(e, "Background device registration failed: ${e.message}")
            }
        }

        // Continue inicialização e navegação imediatamente (não aguardamos o registro)
        lifecycleScope.launch {
            // Se não tem permissões, vai para PermissionsLoginActivity
            if (!hasAllPermissions()) {
                startActivity(Intent(this@MainActivity, LoginPermissionsActivity::class.java))
                finish()
                return@launch
            }

            val user = userDao.getAnyUserOnce()

            if (user?.isLogged == true) {
                startActivity(Intent(this@MainActivity, LoadingDownloadFragmentActivity::class.java))
                finish()
            } else {
                SessionPrefsManagerUtils.clearAll()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }

            Timber.tag("MainActivity").d("Usuário encontrado no DB: ${user?.id}")

        }
    }

    override fun onStart() {
        super.onStart()

        connectivityMonitor = ConnectivityMonitor(appContext, handler).apply {
            onConnectionChanged = { isConnected ->
                connectionStatusBar?.show(isConnected)
                Timber.tag("NetworkStatus").d("Connection changed: $isConnected")
                connectionStatusBar?.show(isConnected)

                // Atualiza outros componentes conforme necessário
                if (isConnected) {
                    // Reconectado - pode sincronizar dados pendentes
                    handler.post {
                        Toast.makeText(appContext, "Conectado à internet", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Desconectado
                    handler.post {
                        Toast.makeText(appContext, "Sem conexão com a internet", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        connectivityMonitor?.cleanup()
        connectionStatusBar?.dismiss()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityMonitor?.cleanup()
        connectionStatusBar?.dismiss()
        handler.removeCallbacksAndMessages(null)
    }
}