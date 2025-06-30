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

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import coil3.load
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.SheetDeviceMiuiBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceMiuiSheet : BaseDialogSheet<SheetDeviceMiuiBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgIcon.load(R.drawable.ic_xiaomi_logo) {
            transformations(CircleCropTransformation())
        }

        binding.btnPrimary.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            } catch (e: Exception) {
                toast(R.string.toast_developer_setting_failed)
            }
        }

        binding.btnSecondary.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}
