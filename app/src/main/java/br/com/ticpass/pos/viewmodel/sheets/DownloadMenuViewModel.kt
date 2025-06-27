/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package br.com.ticpass.pos.viewmodel.sheets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.data.helper.DownloadHelper
import br.com.ticpass.pos.data.installer.AppInstaller
import br.com.ticpass.pos.data.room.download.Download
import br.com.ticpass.pos.data.work.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DownloadMenuViewModel @Inject constructor(
    val downloadHelper: DownloadHelper,
    val appInstaller: AppInstaller
) : ViewModel() {

    fun copyDownloadedApp(context: Context, download: Download, uri: Uri) {
        ExportWorker.exportDownloadedApp(context, download, uri)
    }
}
