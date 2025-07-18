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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import br.com.ticpass.pos.view.ui.login.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import br.com.ticpass.Constants.ALERT_DUE_PAYMENTS_INTERVAL
import br.com.ticpass.Constants.CHECK_DUE_PAYMENTS_INTERVAL
import br.com.ticpass.Constants.EVENT_SYNC_INTERVAL
import br.com.ticpass.Constants.POS_SYNC_INTERVAL
import br.com.ticpass.Constants.REMOVE_OLD_RECORDS_INTERVAL
import br.com.ticpass.Constants.TELEMETRY_INTERVAL
import br.com.ticpass.Constants.getAppName
import br.com.ticpass.pos.data.acquirers.workers.jobs.syncEvent
import br.com.ticpass.pos.data.acquirers.workers.jobs.syncPos
import br.com.ticpass.pos.data.activity.ProductsActivity
import br.com.ticpass.pos.data.event.ForYouViewModel
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.service.GPSService
import br.com.ticpass.pos.view.ui.permissions.PermissionsActivity
import com.airbnb.lottie.compose.LottieConstants
import com.topjohnwu.superuser.internal.UiThreadHandler.handler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import kotlin.getValue
import kotlin.toString
//import stone.utils.Stone


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
                    syncPos(forYouViewModel) {}
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
                                    serial = Build.SERIAL,
                                    coords = "${location.latitude}, ${location.longitude}",
                                    posId = if(data.pos != null) data.pos.id.toInt() else null,
                                    eventId = if(data.event != null) data.event.id.toInt() else null,
                                    cashier = if(data.cashier.isNotEmpty()) data.cashier else null,
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

//class AlertDuePaymentsRunnable @Inject constructor(
//    private val handler: Handler,
//    private val cartViewModel: CartViewModel,
//    private val forYouViewModel: ForYouViewModel,
//): Runnable {
//    override fun run() {
//        val runnable = this
//        val defaultScope = CoroutineScope(Dispatchers.Default)
//        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
//
//        defaultScope.launch(exceptionHandler) {
//            try{
//                val authManager = AuthManager(MainActivity.appContext.dataStore)
//
//                val data = withContext(Dispatchers.IO) {
//                    val membershipExp = authManager.getMembership()
//
//                    object {
//                        val membershipExp = membershipExp
//                    }
//                }
//
//                if(data.membershipExp == null) return@launch
//
//                // check if membership is due
//                val now = Date()
//                val isDue = data.membershipExp?.before(now) == true
//                val daysDue = if(isDue) (now.time - data.membershipExp?.time!!) / (1000 * 60 * 60 * 24) else 0
//
//                if(isDue) {
////                    if(daysDue >= MAX_DUE_PAYMENTS_DAYS) {
////                        // logout user
////
////                        return@launch
////                    }
//
//                    cartViewModel.setPaymentState(
//                        PaymentDialogState(
//                            isDismissable = false,
//                            isDismissed = false,
//                            status = "Assinatura Atrasada",
//                            color = PaymentDialogColor.ERROR,
//                            icon = {
//                                LottieIcon(
//                                    resId = R.raw.error1,
//                                    onStart = {},
//                                    onEnd = {},
//                                    iterations = LottieConstants.IterateForever,
//                                    speed = 3f,
//                                    contentScale = ContentScale.FillHeight,
//                                    modifier = Modifier
//                                        .heightIn(max = 250.dp)
//                                )
//
//                                Column(
//                                    verticalArrangement = Arrangement.Center,
//                                    horizontalAlignment = Alignment.CenterHorizontally,
//                                    modifier = Modifier
//                                        .width(360.dp)
//                                ) {
//                                    Text(
//                                        text = "Regularize sua assinatura.",
//                                        style = MaterialTheme.typography.labelSmall,
//                                        color = Color.LightGray,
//                                        fontSize = 12.sp,
//                                        textAlign = TextAlign.Center,
//                                    )
//                                }
//                            },
//                            actions = {
//                                Box(
//                                    modifier = Modifier
//                                        .noRippleClickable {
//                                            cartViewModel.setPaymentState(
//                                                PaymentDialogState(
//                                                    isDismissed = true,
//                                                    color = PaymentDialogColor.ERROR,
//                                                    status = ""
//                                                )
//                                            )
//                                        }
//                                        .padding(10.dp)
//                                ) {
//                                    Text(
//                                        color = Color.LightGray,
//                                        text = "compreendo!",
//                                        style = MaterialTheme.typography.titleSmall
//                                    )
//                                }
//                            }
//                        )
//                    )
//                }
//            }
//            catch (e: Exception) {
//                Log.d("duePayment", e.toString())
//            }
//            finally {
//                handler.postDelayed(runnable, (ALERT_DUE_PAYMENTS_INTERVAL * 60) * 1000)
//                // defaultScope.cancel()
//            }
//        }
//    }
//}


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val forYouViewModel: ForYouViewModel by viewModels()

    private var gpsTaskRunnable: MyGPSTaskRunnable? = null

    private var backgroundTaskRunnable: POSSYNCTaskRunnable? = null
    private var clearOldEntitiesTaskRunnable: ClearOldEntitiesTaskRunnable? = null
    private var syncEventTaskRunnable: SyncEventTaskRunnable? = null
    private var checkDuePaymentsRunnable: CheckDuePaymentsRunnable? = null
//    private var alertDuePaymentsRunnable: AlertDuePaymentsRunnable? = null


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

//        val appName = getAppName(this)
//        when(BuildConfig.FLAVOR) {
//            "stone" -> {
//                Stone.setAppName(appName)
//            }
//            else -> {}
//        }

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

//        alertDuePaymentsRunnable = AlertDuePaymentsRunnable(handler, cartViewModel, forYouViewModel)
//        handler.post(alertDuePaymentsRunnable as AlertDuePaymentsRunnable)

    }

    override fun onStop() {
        super.onStop()

        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacksAndMessages(null)
    }

}