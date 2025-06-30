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

package br.com.ticpass.pos.compose.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import br.com.ticpass.pos.compose.ui.commons.BlacklistScreen

/**
 * Navigation graph for compose screens
 * @param navHostController [NavHostController] to navigate with compose
 * @param startDestination Starting destination for the activity/app
 */
@Composable
fun NavGraph(navHostController: NavHostController, startDestination: Screen) {
    // TODO: Drop this logic once everything is in compose
    val activity = LocalActivity.current
    fun onNavigateUp() {
        if (navHostController.previousBackStackEntry != null) {
            navHostController.navigateUp()
        } else {
            activity?.finish()
        }
    }

    NavHost(navController = navHostController, startDestination = startDestination) {
        composable<Screen.Blacklist> {
            BlacklistScreen(onNavigateUp = { onNavigateUp() })
        }
    }
}
