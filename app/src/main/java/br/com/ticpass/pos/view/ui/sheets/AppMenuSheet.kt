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

package br.com.ticpass.pos.view.ui.sheets

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import br.com.ticpass.extensions.openInfo
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.event.BusEvent
import br.com.ticpass.pos.data.installer.AppInstaller
import br.com.ticpass.pos.databinding.SheetAppMenuBinding
import br.com.ticpass.pos.util.PackageUtil
import br.com.ticpass.pos.viewmodel.sheets.AppMenuViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppMenuSheet : BaseDialogSheet<SheetAppMenuBinding>() {

    companion object {
        const val TAG = "APP_MENU_SHEET"
    }

    private val viewModel: AppMenuViewModel by viewModels()
    private val args: AppMenuSheetArgs by navArgs()

    private val exportMimeType = "application/zip"

    private val requestDocumentCreation =
        registerForActivityResult(ActivityResultContracts.CreateDocument(exportMimeType)) {
            if (it != null) {
                viewModel.copyInstalledApp(requireContext(), args.app, it)
            } else {
                toast(R.string.failed_apk_export)
            }
            dismissAllowingStateLoss()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isBlacklisted: Boolean = viewModel.blacklistProvider.isBlacklisted(args.app.packageName)

        with(binding.navigationView) {
            //Switch strings for Add/Remove Blacklist
            val blackListMenu: MenuItem = menu.findItem(R.id.action_blacklist)
            blackListMenu.setTitle(
                if (isBlacklisted)
                    R.string.action_whitelist
                else
                    R.string.action_blacklist_add
            )

            //Show/Hide actions based on installed status
            val installed = PackageUtil.isInstalled(requireContext(), args.app.packageName)
            menu.findItem(R.id.action_uninstall).isVisible = installed
            menu.findItem(R.id.action_local).isVisible = installed

            setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.action_blacklist -> {

                        if (isBlacklisted) {
                            viewModel.blacklistProvider.whitelist(args.app.packageName)
                            requireContext().toast(R.string.toast_apk_whitelisted)
                        } else {
                            viewModel.blacklistProvider.blacklist(args.app.packageName)
                            requireContext().toast(R.string.toast_apk_blacklisted)
                        }

                        dismissAllowingStateLoss()
                        AuroraApp.events.send(
                            BusEvent.Blacklisted(args.app.packageName)
                        )
                    }

                    R.id.action_local -> {
                        requestDocumentCreation.launch("${args.app.packageName}.zip")
                    }

                    R.id.action_uninstall -> {
                        AppInstaller.uninstall(requireContext(), args.app.packageName)
                        dismissAllowingStateLoss()
                    }

                    R.id.action_info -> {
                        requireContext().openInfo(args.app.packageName)
                        dismissAllowingStateLoss()
                    }
                }
                false
            }
        }
    }
}
