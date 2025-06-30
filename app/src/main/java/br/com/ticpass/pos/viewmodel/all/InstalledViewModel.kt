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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.web.WebAppDetailsHelper
import br.com.ticpass.pos.data.providers.BlacklistProvider
import br.com.ticpass.pos.data.room.favourite.Favourite
import br.com.ticpass.pos.data.room.favourite.ImportExport
import br.com.ticpass.pos.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class InstalledViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blacklistProvider: BlacklistProvider,
    private val json: Json,
    private val webAppDetailsHelper: WebAppDetailsHelper
) : ViewModel() {

    private val TAG = InstalledViewModel::class.java.simpleName

    private val _apps = MutableStateFlow<List<App>?>(null)
    val apps = _apps.asStateFlow()

    init {
        fetchApps()
    }

    fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val packages = PackageUtil.getAllValidPackages(context)
                    .filterNot { blacklistProvider.isBlacklisted(it.packageName) }

                // Divide the list of packages into chunks of 100 & fetch app details
                // 50 is a safe number to avoid hitting the rate limit or package size limit
                val chunkedPackages = packages.chunked(50)
                val allApps = chunkedPackages.flatMap { chunk ->
                    webAppDetailsHelper.getAppDetails(chunk.map { it.packageName })
                }

                _apps.emit(allApps)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
            }
        }
    }

    fun exportApps(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favourites: List<Favourite> = apps.value!!.map { app ->
                    Favourite.fromApp(app, Favourite.Mode.IMPORT)
                }
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.encodeToString(ImportExport(favourites)).encodeToByteArray())
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to installed apps", exception)
            }
        }
    }
}
