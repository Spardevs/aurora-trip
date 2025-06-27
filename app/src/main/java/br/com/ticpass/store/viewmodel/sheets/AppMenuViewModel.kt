/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package br.com.ticpass.store.viewmodel.sheets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import br.com.ticpass.store.data.model.MinimalApp
import br.com.ticpass.store.data.providers.BlacklistProvider
import br.com.ticpass.store.data.work.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppMenuViewModel @Inject constructor(val blacklistProvider: BlacklistProvider) : ViewModel() {

    fun copyInstalledApp(context: Context, app: MinimalApp, uri: Uri) {
        ExportWorker.exportInstalledApp(context, app, uri)
    }
}
