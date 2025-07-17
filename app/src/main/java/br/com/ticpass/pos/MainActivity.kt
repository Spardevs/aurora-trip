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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.preferencesDataStore
import br.com.ticpass.pos.view.ui.login.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import br.com.ticpass.pos.data.activity.ProductsActivity
import br.com.ticpass.pos.view.ui.permissions.PermissionsActivity

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user")

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        lateinit  var location: Location
        lateinit  var appContext: Context
        @SuppressLint("StaticFieldLeak")
        lateinit  var activity: Activity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            prefs.edit().putBoolean("isFirstRun", false).apply()
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
            return
        }

        val hasToken = prefs.contains("auth_token")
        var intent: Intent
        if (!hasToken) {
            intent = Intent(this, LoginScreen::class.java)
        } else {
            intent = Intent(this, ProductsActivity::class.java)
        }

        startActivity(intent)
        finish()
    }

}