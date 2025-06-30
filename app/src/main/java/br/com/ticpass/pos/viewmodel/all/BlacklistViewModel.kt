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
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.data.event.BusEvent
import br.com.ticpass.pos.data.helper.UpdateHelper
import br.com.ticpass.pos.data.providers.BlacklistProvider
import br.com.ticpass.pos.util.CertUtil
import br.com.ticpass.pos.util.PackageUtil
import br.com.ticpass.pos.util.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class BlacklistViewModel @Inject constructor(
    private val json: Json,
    private val updateHelper: UpdateHelper,
    private val blacklistProvider: BlacklistProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = BlacklistViewModel::class.java.simpleName

    private val isAuroraOnlyFilterEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_AURORA_ONLY, false)
    private val isFDroidFilterEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_FDROID, true)
    private val isExtendedUpdateEnabled =
        Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    private val _packages = MutableStateFlow<List<PackageInfo>?>(null)
    private val _filteredPackages = MutableStateFlow<List<PackageInfo>?>(null)
    val filteredPackages = _filteredPackages.asStateFlow()

    val blacklist = mutableStateListOf<String>()

    init {
        blacklist.addAll(blacklistProvider.blacklist)
        fetchApps()
    }

    private fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _packages.value = PackageUtil.getAllValidPackages(context).also { pkgList ->
                    _filteredPackages.value = pkgList
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
            }
        }
    }

    fun search(query: String) {
        if (query.isNotBlank()) {
            _filteredPackages.value = _packages.value!!
                .filter { it.applicationInfo!!.loadLabel(context.packageManager)
                    .contains(query, true) || it.packageName.contains(query, true)
                }
        } else {
            _filteredPackages.value = _packages.value
        }
    }

    fun isFiltered(packageInfo: PackageInfo): Boolean {
        return when {
            !isExtendedUpdateEnabled && !packageInfo.applicationInfo!!.enabled -> true
            isAuroraOnlyFilterEnabled -> !CertUtil.isAuroraStoreApp(context, packageInfo.packageName)
            isFDroidFilterEnabled -> CertUtil.isFDroidApp(context, packageInfo.packageName)
            else -> false
        }
    }

    fun blacklist(packageName: String) {
        blacklist.add(packageName)
        blacklistProvider.blacklist(packageName)
        AuroraApp.events.send(BusEvent.Blacklisted(packageName))
    }

    fun blacklistAll() {
        blacklistProvider.blacklist = _packages.value!!.map { it.packageName }.toMutableSet()
        blacklist.apply {
            clear()
            addAll(blacklistProvider.blacklist)
        }
        viewModelScope.launch { updateHelper.deleteAllUpdates() }
    }

    fun whitelist(packageName: String) {
        blacklist.remove(packageName)
        blacklistProvider.whitelist(packageName)
    }

    fun whitelistAll() {
        blacklist.clear()
        blacklistProvider.blacklist = mutableSetOf()
    }

    fun importBlacklist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    val importedSet = json.decodeFromString<MutableSet<String>>(
                        it.bufferedReader().readText()
                    )

                    val validImportedSet = importedSet
                        .filter { pkgName -> _packages.value!!.any { it.packageName == pkgName } }
                    blacklistProvider.blacklist.addAll(validImportedSet)
                    blacklist.apply {
                        clear()
                        addAll(blacklistProvider.blacklist)
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to import blacklist", exception)
            }
        }
    }

    fun exportBlacklist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.encodeToString(blacklistProvider.blacklist).encodeToByteArray())
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to export blacklist", exception)
            }
        }
    }
}
