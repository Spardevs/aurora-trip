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
import android.view.View
import androidx.navigation.fragment.navArgs
import coil3.load
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import br.com.ticpass.extensions.copyToClipBoard
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.SheetInstallErrorBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstallErrorDialogSheet : BaseDialogSheet<SheetInstallErrorBinding>() {

    private val args: InstallErrorDialogSheetArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgIcon.load(args.app.iconArtwork.url) {
            transformations(CircleCropTransformation())
        }

        binding.txtLine1.text = args.app.displayName
        binding.txtLine2.text = args.error
        binding.txtLine3.text = args.extra

        binding.btnPrimary.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnSecondary.setOnClickListener {
            requireContext().copyToClipBoard(args.extra)
            requireContext().toast(R.string.toast_clipboard_copied)
        }
    }
}
