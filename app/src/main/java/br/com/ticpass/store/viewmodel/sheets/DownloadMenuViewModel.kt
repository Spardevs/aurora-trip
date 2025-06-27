/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package br.com.ticpass.store.viewmodel.sheets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import br.com.ticpass.store.data.helper.DownloadHelper
import br.com.ticpass.store.data.installer.AppInstaller
import br.com.ticpass.store.data.room.download.Download
import br.com.ticpass.store.data.work.ExportWorker
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
