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

package br.com.ticpass.pos.viewmodel.all

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.helper.DownloadHelper
import br.com.ticpass.pos.data.helper.UpdateHelper
import br.com.ticpass.pos.data.room.update.Update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    val updateHelper: UpdateHelper,
    private val downloadHelper: DownloadHelper,
) : ViewModel() {

    var updateAllEnqueued: Boolean = false

    val downloadsList get() = downloadHelper.downloadsList
    val updates get() = updateHelper.updates

    val fetchingUpdates = updateHelper.isCheckingUpdates

    fun fetchUpdates() {
        updateHelper.checkUpdatesNow()
    }

    fun download(update: Update) {
        viewModelScope.launch { downloadHelper.enqueueUpdate(update) }
    }

    fun downloadAll() {
        viewModelScope.launch {
            updates.value?.forEach { downloadHelper.enqueueUpdate(it) }
        }
    }

    fun cancelDownload(packageName: String) {
        viewModelScope.launch { downloadHelper.cancelDownload(packageName) }
    }

    fun cancelAll() {
        viewModelScope.launch { downloadHelper.cancelAll(true) }
    }
}
