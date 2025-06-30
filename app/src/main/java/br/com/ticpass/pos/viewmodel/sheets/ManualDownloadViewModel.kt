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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.PurchaseHelper
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.data.event.BusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualDownloadViewModel @Inject constructor(
    private val purchaseHelper: PurchaseHelper
) : ViewModel() {

    private val TAG = ManualDownloadViewModel::class.java.simpleName

    private val _purchaseStatus = MutableSharedFlow<Boolean>()
    val purchaseStatus = _purchaseStatus.asSharedFlow()

    fun purchase(app: App, customVersion: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = purchaseHelper.purchase(app.packageName, customVersion, app.offerType)
                if (files.isNotEmpty()) {
                    AuroraApp.events.send(
                        BusEvent.ManualDownload(app.packageName, customVersion)
                    )
                }
                _purchaseStatus.emit(files.isNotEmpty())
            } catch (exception: Exception) {
                _purchaseStatus.emit(false)
                Log.e(TAG, "Failed to find version: $customVersion", exception)
            }
        }
    }
}
