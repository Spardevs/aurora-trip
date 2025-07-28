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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.preferencesDataStore
import br.com.ticpass.pos.view.ui.login.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.firebase.analytics.FirebaseAnalytics

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