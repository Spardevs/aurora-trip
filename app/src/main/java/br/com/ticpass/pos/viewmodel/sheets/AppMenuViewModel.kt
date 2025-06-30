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

package br.com.ticpass.pos.viewmodel.sheets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.data.model.MinimalApp
import br.com.ticpass.pos.data.providers.BlacklistProvider
import br.com.ticpass.pos.data.work.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppMenuViewModel @Inject constructor(val blacklistProvider: BlacklistProvider) : ViewModel() {

    fun copyInstalledApp(context: Context, app: MinimalApp, uri: Uri) {
        ExportWorker.exportInstalledApp(context, app, uri)
    }
}
