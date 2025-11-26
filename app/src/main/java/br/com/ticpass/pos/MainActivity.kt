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
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import br.com.ticpass.pos.core.util.ConnectionStatusBar
import br.com.ticpass.pos.core.util.ConnectivityMonitor
import br.com.ticpass.pos.core.util.DeviceUtils
import br.com.ticpass.pos.data.user.repository.UserRepository
import br.com.ticpass.pos.presentation.login.activities.LoginActivity
import br.com.ticpass.pos.presentation.login.activities.PermissionsLoginActivity
import br.com.ticpass.pos.presentation.shared.activities.BaseActivity
import com.topjohnwu.superuser.internal.UiThreadHandler.handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private var connectivityMonitor: ConnectivityMonitor? = null
    private var connectionStatusBar: ConnectionStatusBar? = null

    lateinit var userRepository: UserRepository

    companion object {
        lateinit  var location: Location
        lateinit  var appContext: Context
        @SuppressLint("StaticFieldLeak")
        lateinit  var activity: Activity
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
    private fun logDeviceInfo() {
        try {
            val model = DeviceUtils.getDeviceModel()
            val acquirer = DeviceUtils.getAcquirer()
            val serial = DeviceUtils.getDeviceSerial(this)

            Log.i("DeviceInfo", "═══════════════════════════════════════════")
            Log.i("DeviceInfo", "          DEVICE INFORMATION")
            Log.i("DeviceInfo", "═══════════════════════════════════════════")
            Log.i("DeviceInfo", "Model:       $model")
            Log.i("DeviceInfo", "Acquirer:    $acquirer")
            Log.i("DeviceInfo", "Serial:      $serial")
            Log.i("DeviceInfo", "═══════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e("DeviceInfo", "Error logging device info: ${e.message}", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = applicationContext
        activity = this

        // Initialize Acquirer SDK first
        // Log device information
        logDeviceInfo()

        connectionStatusBar = ConnectionStatusBar(this)


        if (!hasAllPermissions()) {
            startActivity(Intent(this, PermissionsLoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                try {
                    userRepository.getLoggedUser()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao acessar DB: ${e.message}", e)
                    null
                }
            }

            if (user == null) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            } else {
                Log.d("MainActivity", "Usuário encontrado no DB: ${user.id}")
                // continuar inicialização normal
            }
        }

        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()

        connectivityMonitor = ConnectivityMonitor(appContext, handler).apply {
            onConnectionChanged = { isConnected ->
                connectionStatusBar?.show(isConnected)
                Log.d("NetworkStatus", "Connection changed: $isConnected")
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