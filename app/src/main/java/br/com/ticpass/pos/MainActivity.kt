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
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.lifecycle.lifecycleScope
import androidx.navigation.FloatingWindow
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import br.com.ticpass.pos.data.model.NetworkStatus
import br.com.ticpass.pos.data.receiver.MigrationReceiver
import br.com.ticpass.pos.databinding.ActivityMainBinding
import br.com.ticpass.pos.util.PackageUtil
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import br.com.ticpass.pos.view.ui.sheets.NetworkDialogSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var B: ActivityMainBinding

    // TopLevelFragments
    private val topLevelFrags = listOf(
        R.id.appsContainerFragment,
        R.id.gamesContainerFragment,
        R.id.updatesFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Check and run migrations first if required
        // This is needed thanks to OEMs breaking the MY_PACKAGE_REPLACED API
        MigrationReceiver.runMigrationsIfRequired(this)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        B = ActivityMainBinding.inflate(layoutInflater)
        setContentView(B.root)

        // Adjust root view's paddings for edgeToEdge display
        ViewCompat.setOnApplyWindowInsetsListener(B.root) { root, windowInsets ->
            val insets = windowInsets.getInsets(systemBars() or displayCutout() or ime())
            root.setPadding(insets.left, insets.top, insets.right, 0)
            windowInsets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (!PackageUtil.isTv(this)) {
            viewModel.networkProvider.status.onEach { networkStatus ->
                when (networkStatus) {
                    NetworkStatus.AVAILABLE -> {
                        if (!supportFragmentManager.isDestroyed && isIntroDone()) {
                            val fragment = supportFragmentManager
                                .findFragmentByTag(NetworkDialogSheet.TAG)
                            fragment?.let {
                                supportFragmentManager.beginTransaction()
                                    .remove(fragment)
                                    .commitAllowingStateLoss()
                            }
                        }

                    }

                    NetworkStatus.UNAVAILABLE -> {
                        if (!supportFragmentManager.isDestroyed && isIntroDone()) {
                            supportFragmentManager.beginTransaction()
                                .add(NetworkDialogSheet.newInstance(), NetworkDialogSheet.TAG)
                                .commitAllowingStateLoss()
                        }
                    }
                }
            }.launchIn(AuroraApp.scope)
        }

        B.navView.setupWithNavController(navController)

        // Handle quick exit from back actions
        val defaultTab = when (Preferences.getInteger(this, PREFERENCE_DEFAULT_SELECTED_TAB)) {
            1 -> R.id.gamesContainerFragment
            2 -> R.id.updatesFragment
            else -> R.id.appsContainerFragment
        }
        onBackPressedDispatcher.addCallback(this) {
            if (navController.currentDestination?.id in topLevelFrags) {
                if (navController.currentDestination?.id == defaultTab) {
                    finish()
                } else {
                    navController.navigate(defaultTab)
                }
            } else if (navHostFragment.childFragmentManager.backStackEntryCount == 0) {
                // We are on either on onboarding or splash fragment
                finish()
            } else {
                navController.navigateUp()
            }
        }

        // Handle views on fragments
        navController.addOnDestinationChangedListener { _, navDestination, _ ->
            if (navDestination !is FloatingWindow) {
                when (navDestination.id) {
                    in topLevelFrags -> B.navView.visibility = View.VISIBLE
                    else -> B.navView.visibility = View.GONE
                }
            }
        }

        // Updates
        lifecycleScope.launch {
            viewModel.updateHelper.updates.collectLatest { list ->
                B.navView.getOrCreateBadge(R.id.updatesFragment).apply {
                    isVisible = !list.isNullOrEmpty()
                    number = list?.size ?: 0
                }
            }
        }
    }

    private fun isIntroDone(): Boolean {
        return Preferences.getBoolean(this@MainActivity, Preferences.PREFERENCE_INTRO)
    }
}
