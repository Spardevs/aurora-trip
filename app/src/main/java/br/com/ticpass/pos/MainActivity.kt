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
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build.SERIAL
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import br.com.ticpass.pos.view.ui.login.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.Constants.CHECK_DUE_PAYMENTS_INTERVAL
import br.com.ticpass.Constants.EVENT_SYNC_INTERVAL
import br.com.ticpass.Constants.POS_SYNC_INTERVAL
import br.com.ticpass.Constants.REMOVE_OLD_RECORDS_INTERVAL
import br.com.ticpass.Constants.TELEMETRY_INTERVAL
import br.com.ticpass.pos.data.acquirers.workers.jobs.syncEvent
import br.com.ticpass.pos.data.acquirers.workers.jobs.syncPos
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.data.activity.ProductsActivity
import br.com.ticpass.pos.data.event.ForYouViewModel
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.service.GPSService
import br.com.ticpass.pos.util.ConnectionStatusBar
import br.com.ticpass.pos.data.activity.PermissionsActivity
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.api.Api2Repository
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.pos.util.ConnectivityMonitor
import br.com.ticpass.pos.util.DeviceUtils
import com.topjohnwu.superuser.internal.UiThreadHandler.handler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.getValue
import kotlin.toString

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user")

class POSSYNCTaskRunnable @Inject constructor(
    private val handler: Handler,
    private val forYouViewModel: ForYouViewModel,
): Runnable {
    override fun run() {
        val runnable = this

        val defaultScope = CoroutineScope(Dispatchers.Default)
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(exceptionHandler) {
            try{
                withContext(Dispatchers.Default) {
                    syncPos(
                        forYouViewModel = forYouViewModel,
                        onProgress = {},
                        onFailure = {},
                        onDone = {}
                    )

                }
            }
            catch (e: Exception) {
                Log.d("POSSYNCTaskRunnable", e.toString())
            }
            handler.postDelayed(runnable, (POS_SYNC_INTERVAL * 60) * 1000)
        }
    }
}

class ClearOldEntitiesTaskRunnable @Inject constructor(
    private val handler: Handler,
    private val forYouViewModel: ForYouViewModel,
): Runnable {
    override fun run() {
        val runnable = this

        val defaultScope = CoroutineScope(Dispatchers.Default)
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(exceptionHandler) {
            try{
                withContext(Dispatchers.IO) {
                    forYouViewModel.orderRepository.deleteOld()
                    forYouViewModel.paymentRepository.deleteOld()
                    forYouViewModel.cashupRepository.deleteOld()
                    forYouViewModel.voucherRepository.deleteOld()
                    forYouViewModel.voucherRedemptionRepository.deleteOld()
                    forYouViewModel.refundRepository.deleteOld()
                    forYouViewModel.acquisitionRepository.deleteOld()
                    forYouViewModel.consumptionRepository.deleteOld()
                    forYouViewModel.passRepository.deleteOld()
                }
            }
            catch (e: Exception) {
                Log.d("ClearOldEntitiesTaskRunnable", e.toString())
            }
            handler.postDelayed(runnable, (REMOVE_OLD_RECORDS_INTERVAL * 60) * 1000)
        }
    }
}

class SyncEventTaskRunnable @Inject constructor(
    private val handler: Handler,
    private val forYouViewModel: ForYouViewModel,
): Runnable {
    override fun run() {
        val runnable = this

        val defaultScope = CoroutineScope(Dispatchers.Default)
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(exceptionHandler) {
            try{
                val selectedEvent = withContext(Dispatchers.IO) {
                    forYouViewModel.eventRepository.getSelectedEvent()
                }

                if(selectedEvent == null){
                    throw IllegalArgumentException("No Event selected")
                }

                val doSyncEvent = withContext(Dispatchers.Default) {
                    syncEvent(
                        forYouViewModel,
                        false,
                        onFailure = {
                            throw IllegalArgumentException("Failed to sync Event")
                        }
                    )
                }
            }
            catch (e: Exception) {
                Log.d("SyncEventTaskRunnable", e.toString())
            }

            handler.postDelayed(runnable, (EVENT_SYNC_INTERVAL * 60) * 1000)
        }
    }
}

class MyGPSTaskRunnable @Inject constructor(
    private val handler: Handler,
    private val forYouViewModel: ForYouViewModel,
): Runnable {
    override fun run() {
        val runnable = this

        try{

            val defaultScope = CoroutineScope(Dispatchers.Default)
            val exceptionHandler = CoroutineExceptionHandler { _, _ -> }

            defaultScope.launch(exceptionHandler) {
                val data = withContext(Dispatchers.IO) {
                    val authManager = AuthManager(MainActivity.appContext.dataStore)
                    val gps = GPSService(MainActivity.appContext) { lat, long -> }
                    val event = forYouViewModel.eventRepository.getSelectedEvent()
                    val pos = forYouViewModel.posRepository.getSelectedPos()
                    val cashier = authManager.getCashierName()

                    object {
                        val event = event
                        val pos = pos
                        val cashier = cashier
                        val gps = gps
                    }
                }

                val doPingDevice = withContext(Dispatchers.IO) {
                    data.gps.getUserCoordinates() { location ->
                        MainActivity.location = location

                        defaultScope.launch(exceptionHandler) {
                            withContext(Dispatchers.IO) {
                                forYouViewModel.apiRepository.pingDevice(
                                    serial = SERIAL,
                                    coords = "${location.latitude}, ${location.longitude}",
                                    posId = data.pos.id.toInt(),
                                    eventId = data.event?.id?.toInt(),
                                    cashier = data.cashier.ifEmpty { null },
                                )
                            }
                        }
                    }
                }

                handler.postDelayed(runnable, (TELEMETRY_INTERVAL * 60) * 1000)
            }
        }
        catch (e: Exception) {
            Log.d("POSSYNCTaskRunnable", e.toString())
        }
    }
}

class CheckDuePaymentsRunnable @Inject constructor(
    private val handler: Handler,
    private val forYouViewModel: ForYouViewModel,
): Runnable {
    override fun run() {
        val runnable = this
        val defaultScope = CoroutineScope(Dispatchers.Default)
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(exceptionHandler) {
            try{
                val authManager = AuthManager(MainActivity.appContext.dataStore)

                val data = withContext(Dispatchers.IO) {
                    val token = authManager.getJwtToken()

                    object {
                        val token = token
                    }
                }

                if(data.token.isEmpty()) return@launch

                val membershipResponse = withContext(Dispatchers.IO) {
                    forYouViewModel.apiRepository.getMembership(
                        data.token,
                    )
                }

                withContext(Dispatchers.IO) {
                    authManager.setMembership(membershipResponse.result.expiration)
                }

                Log.d("duePayment", membershipResponse.toString())
            }
            catch (e: Exception) {
                Log.d("duePayment", e.toString())
            }
            finally {
                handler.postDelayed(runnable, (CHECK_DUE_PAYMENTS_INTERVAL * 60) * 1000)
                // defaultScope.cancel()
            }
        }
    }
}

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private val forYouViewModel: ForYouViewModel by viewModels()

    private var gpsTaskRunnable: MyGPSTaskRunnable? = null

    private var backgroundTaskRunnable: POSSYNCTaskRunnable? = null
    private var clearOldEntitiesTaskRunnable: ClearOldEntitiesTaskRunnable? = null
    private var syncEventTaskRunnable: SyncEventTaskRunnable? = null
    private var checkDuePaymentsRunnable: CheckDuePaymentsRunnable? = null
//    private var alertDuePaymentsRunnable: AlertDuePaymentsRunnable? = null

    @Inject
    lateinit var apiRepository: APIRepository

    @Inject
    lateinit var api2Repository: Api2Repository

    private var connectivityMonitor: ConnectivityMonitor? = null
    private var connectionStatusBar: ConnectionStatusBar? = null


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
            val stoneCode = if (AcquirerSdk.isInitialized()) {
                AcquirerSdk.getStoneCode()
            } else {
                "SDK not initialized"
            }

            Log.i("DeviceInfo", "═══════════════════════════════════════════")
            Log.i("DeviceInfo", "          DEVICE INFORMATION")
            Log.i("DeviceInfo", "═══════════════════════════════════════════")
            Log.i("DeviceInfo", "Model:       $model")
            Log.i("DeviceInfo", "Acquirer:    $acquirer")
            Log.i("DeviceInfo", "Serial:      $serial")
            Log.i("DeviceInfo", "Stone Code:  ${if (stoneCode.isEmpty()) "N/A" else stoneCode}")
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
        AcquirerSdk.initialize(applicationContext)

        // Log device information
        logDeviceInfo()

        connectionStatusBar = ConnectionStatusBar(this)


        if (!hasAllPermissions()) {
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
            return
        }

        val prefs =
            getSharedPreferences("UserPrefs", MODE_PRIVATE)

        val hasLogged = prefs.contains("user_logged")


        Log.d("hasLogged", "$hasLogged")
        val intent: Intent = if (!hasLogged) {
            Intent(this, LoginScreen::class.java)
        } else {
            Intent(this, ProductsActivity::class.java)
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

//    val appName = getAppName(this)
//    when(BuildConfig.FLAVOR) {
//    "stone" -> {
//    Stone.setAppName(appName)
//    }
//    else -> {}
//    }

        backgroundTaskRunnable = POSSYNCTaskRunnable(handler, forYouViewModel)
        handler.post(backgroundTaskRunnable as POSSYNCTaskRunnable)

        gpsTaskRunnable = MyGPSTaskRunnable(handler, forYouViewModel)
        handler.post(gpsTaskRunnable as MyGPSTaskRunnable)

        clearOldEntitiesTaskRunnable = ClearOldEntitiesTaskRunnable(handler, forYouViewModel)
        handler.post(clearOldEntitiesTaskRunnable as ClearOldEntitiesTaskRunnable)

        syncEventTaskRunnable = SyncEventTaskRunnable(handler, forYouViewModel)
        handler.post(syncEventTaskRunnable as SyncEventTaskRunnable)

        checkDuePaymentsRunnable = CheckDuePaymentsRunnable(handler, forYouViewModel)
        handler.post(checkDuePaymentsRunnable as CheckDuePaymentsRunnable)

//    alertDuePaymentsRunnable = AlertDuePaymentsRunnable(handler, cartViewModel, forYouViewModel)
//    handler.post(alertDuePaymentsRunnable as AlertDuePaymentsRunnable)

    }

    fun Application.getCurrentActivity(): Activity? {
        var currentActivity: Activity? = null

        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityPaused(activity: Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        return currentActivity
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