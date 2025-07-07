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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.FloatingWindow
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import br.com.ticpass.pos.data.model.NetworkStatus
import br.com.ticpass.pos.data.receiver.MigrationReceiver
import br.com.ticpass.pos.util.PackageUtil
import br.com.ticpass.pos.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import br.com.ticpass.pos.view.ui.login.LoginScreen
import br.com.ticpass.pos.view.ui.sheets.NetworkDialogSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.analytics.FirebaseAnalytics
import br.com.ticpass.pos.util.ALERT_DUE_PAYMENTS_INTERVAL
import br.com.ticpass.pos.util.APP_NAME
import br.com.ticpass.pos.util.CHECK_DUE_PAYMENTS_INTERVAL
import br.com.ticpass.pos.util.EVENT_SYNC_INTERVAL
import br.com.ticpass.pos.util.POS_SYNC_INTERVAL
import br.com.ticpass.pos.util.REMOVE_OLD_RECORDS_INTERVAL
import br.com.ticpass.pos.util.TELEMETRY_INTERVAL
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

//import stone.utils.Stone
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user")

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        lateinit  var location: Location
        lateinit  var appContext: Context
        lateinit  var activity: Activity
        lateinit var firebaseAnalytics: FirebaseAnalytics

        fun logCustomEventMessage(eventName: String, source: String, message: String) {
            val bundle = Bundle().apply {
                putString("source", source)
                putString("message", message)
            }
            firebaseAnalytics.logEvent(eventName, bundle)
        }

        fun logErrorMessage(source: String, message: String) {
            val bundle = Bundle().apply {
                putString("error_source", source)
                putString("error_message", message)
            }
            firebaseAnalytics.logEvent("app_crash_reported", bundle)
        }

        fun logCrashException(exception: Exception) {
            val bundle = Bundle().apply {
                putString("error_message", exception.message ?: "Unknown error")
                putString("error_type", exception.javaClass.simpleName)
            }
            firebaseAnalytics.logEvent("app_crash_reported", bundle)
        }
    }

    private val _activity: Activity
        get() {
            return this
        }

    override fun onStart() {
        super.onStart()

//        when (BuildConfig.FLAVOR) {
//            "stone" -> {
//                Stone.setAppName(APP_NAME)
//            }
//
//            else -> {}
//        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        firebaseAnalytics = Firebase.analytics

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val currentActivity = this@MainActivity
        location = Location("")
        appContext = applicationContext
        activity = _activity

        if (!isUserLoggedIn()) {
            startActivity(Intent(this, LoginScreen::class.java))
            finish()
            return
        }

    }

    private fun isUserLoggedIn(): Boolean {
        return false
//        return Preferences.getBoolean(this, Preferences.PREFERENCE_IS_LOGGED_IN)
    }
}