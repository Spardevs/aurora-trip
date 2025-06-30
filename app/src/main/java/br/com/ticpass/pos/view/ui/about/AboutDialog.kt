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

package br.com.ticpass.pos.view.ui.about

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import br.com.ticpass.pos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.about_aurora_store_title)
            .setMessage(R.string.about_aurora_store_summary)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> dialog?.dismiss() }
            .create()
    }
}
