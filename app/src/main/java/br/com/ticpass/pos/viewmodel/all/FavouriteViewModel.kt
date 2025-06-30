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
import br.com.ticpass.pos.data.room.favourite.Favourite
import br.com.ticpass.pos.data.room.favourite.FavouriteDao
import br.com.ticpass.pos.data.room.favourite.ImportExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class FavouriteViewModel @Inject constructor(
    private val favouriteDao: FavouriteDao,
    private val json: Json
) : ViewModel() {
    private val TAG = FavouriteViewModel::class.java.simpleName

    val favouritesList = favouriteDao.favourites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun importFavourites(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    val importExport = json.decodeFromString<ImportExport>(
                        it.bufferedReader().readText()
                    )

                    favouriteDao.insertAll(
                        importExport.favourites.map { fav -> fav.copy(mode = Favourite.Mode.IMPORT) }
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to import favourites", exception)
            }
        }
    }

    fun exportFavourites(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(
                        json.encodeToString(ImportExport(favouritesList.value!!))
                            .encodeToByteArray()
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to export favourites", exception)
            }
        }
    }

    fun removeFavourite(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            favouriteDao.delete(packageName)
        }
    }
}
