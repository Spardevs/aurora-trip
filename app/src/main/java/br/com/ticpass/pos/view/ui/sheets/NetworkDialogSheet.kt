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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import br.com.ticpass.extensions.isQAndAbove
import br.com.ticpass.pos.databinding.SheetNetworkBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkDialogSheet : BaseDialogSheet<SheetNetworkBinding>() {

    private val TAG = NetworkDialogSheet::class.java.simpleName

    companion object {

        const val TAG = "NetworkDialogSheet"

        @JvmStatic
        fun newInstance(): NetworkDialogSheet {
            return NetworkDialogSheet().apply {

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnAction.setOnClickListener {
            if (isQAndAbove) {
                startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
            } else {
                try {
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } catch (exception: ActivityNotFoundException) {
                    Log.i(TAG, "Unable to launch wireless settings")
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }
}
