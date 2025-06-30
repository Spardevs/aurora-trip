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

package br.com.ticpass.pos.view.ui.account

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.providers.AccountProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogoutDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_logout_confirmation_title)
            .setMessage(R.string.action_logout_confirmation_message)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> logout() }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> dialog?.dismiss()}
            .create()
    }

    private fun logout() {
        AccountProvider.logout(requireContext())
        findNavController().navigate(LogoutDialogDirections.actionLogoutDialogToSplashFragment())
    }
}
