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

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.SheetManualDownloadBinding
import br.com.ticpass.pos.viewmodel.sheets.ManualDownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManualDownloadSheet : BaseDialogSheet<SheetManualDownloadBinding>() {
    private val viewModel: ManualDownloadViewModel by viewModels()
    private val args: ManualDownloadSheetArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        inflateData()
        attachActions()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.purchaseStatus.collectLatest {
                if (it) {
                    toast(R.string.toast_manual_available)
                    dismissAllowingStateLoss()
                } else {
                    toast(R.string.toast_manual_unavailable)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        this.lifecycleScope.launch {
            viewModel.purchaseStatus.collectLatest {
                if (it) {
                    toast(R.string.toast_manual_available)
                    dismissAllowingStateLoss()
                } else {
                    toast(R.string.toast_manual_unavailable)
                }
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun inflateData() {
        binding.imgIcon.load(args.app.iconArtwork.url) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(32F))
        }

        binding.txtLine1.text = args.app.displayName
        binding.txtLine2.text = args.app.packageName
        binding.txtLine3.text = ("${args.app.versionName} (${args.app.versionCode})")

        binding.versionCodeLayout.hint = "${args.app.versionCode}"
        binding.versionCodeLayout.editText?.setText("${args.app.versionCode}")
    }

    private fun attachActions() {
        binding.btnPrimary.setOnClickListener {
            val customVersionString = (binding.versionCodeInp.text).toString()
            if (customVersionString.isEmpty())
                binding.versionCodeInp.error = "Enter version code"
            else {
                viewModel.purchase(args.app, customVersionString.toLong())
            }
        }

        binding.btnSecondary.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}
