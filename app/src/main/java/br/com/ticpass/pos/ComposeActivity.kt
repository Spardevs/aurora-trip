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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.navigation.compose.rememberNavController
import br.com.ticpass.pos.compose.navigation.Screen
import br.com.ticpass.pos.compose.theme.AuroraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // TODO: Change startDestination logic to mirror MainActivity
        val startDestination = IntentCompat.getParcelableExtra(
            intent,
            Screen.PARCEL_KEY,
            Screen::class.java
        ) ?: Screen.Blacklist

        setContent {
            AuroraTheme {
                val navController = rememberNavController()
            }
        }
    }
}
