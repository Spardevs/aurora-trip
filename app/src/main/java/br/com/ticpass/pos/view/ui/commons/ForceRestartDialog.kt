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

package br.com.ticpass.pos.view.ui.commons

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import br.com.ticpass.pos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForceRestartDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.force_restart_title)
            .setMessage(R.string.force_restart_summary)
            .setPositiveButton(getString(R.string.action_restart)) { _, _ ->
                ProcessPhoenix.triggerRebirth(requireContext())
            }
            .create()
    }

    override fun onResume() {
        super.onResume()
        dialog?.setCancelable(false)
    }
}
