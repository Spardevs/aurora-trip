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
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import br.com.ticpass.extensions.copyToClipBoard
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.DownloadStatus
import br.com.ticpass.pos.databinding.SheetDownloadMenuBinding
import br.com.ticpass.pos.util.PathUtil
import br.com.ticpass.pos.viewmodel.sheets.DownloadMenuViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadMenuSheet : BaseDialogSheet<SheetDownloadMenuBinding>() {

    private val TAG = DownloadMenuSheet::class.java.simpleName

    private val viewModel: DownloadMenuViewModel by viewModels()
    private val args: DownloadMenuSheetArgs by navArgs()
    private val playStoreURL = "https://play.google.com/store/apps/details?id="

    private val exportMimeType = "application/zip"

    private val requestDocumentCreation =
        registerForActivityResult(ActivityResultContracts.CreateDocument(exportMimeType)) {
            if (it != null) {
                viewModel.copyDownloadedApp(requireContext(), args.download, it)
            } else {
                toast(R.string.failed_apk_export)
            }
            dismissAllowingStateLoss()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.navigationView) {
            val downloadCompleted = args.download.downloadStatus == DownloadStatus.COMPLETED
            val downloadDir = PathUtil.getAppDownloadDir(
                requireContext(),
                args.download.packageName,
                args.download.versionCode
            )

            menu.findItem(R.id.action_cancel).isVisible = !args.download.isFinished
            menu.findItem(R.id.action_clear).isVisible = args.download.isFinished
            menu.findItem(R.id.action_install).isVisible = downloadCompleted
            menu.findItem(R.id.action_local).isVisible =
                downloadCompleted && downloadDir.listFiles() != null

            setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.action_install -> {
                        install()
                        dismissAllowingStateLoss()
                    }

                    R.id.action_copy -> {
                        requireContext().copyToClipBoard(
                            "${playStoreURL}${args.download.packageName}"
                        )
                        requireContext().toast(requireContext().getString(R.string.toast_clipboard_copied))
                        dismissAllowingStateLoss()
                    }

                    R.id.action_cancel -> {
                        findViewTreeLifecycleOwner()?.lifecycleScope?.launch(NonCancellable) {
                            viewModel.downloadHelper.cancelDownload(args.download.packageName)
                        }
                        dismissAllowingStateLoss()
                    }

                    R.id.action_clear -> {
                        findViewTreeLifecycleOwner()?.lifecycleScope?.launch(NonCancellable) {
                            viewModel.downloadHelper.clearDownload(
                                args.download.packageName,
                                args.download.versionCode
                            )
                        }
                        dismissAllowingStateLoss()
                    }

                    R.id.action_local -> {
                        requestDocumentCreation.launch("${args.download.packageName}.zip")
                    }
                }
                false
            }
        }
    }

    private fun install() {
        try {
            viewModel.appInstaller.getPreferredInstaller().install(args.download)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to install ${args.download.packageName}", exception)
            if (exception is NullPointerException) {
                requireContext().toast(R.string.installer_status_failure_invalid)
            }
        }
    }
}
