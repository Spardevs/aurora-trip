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

package br.com.ticpass.pos.viewmodel.category

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.helpers.CategoryHelper
import com.aurora.gplayapi.helpers.contracts.CategoryContract
import br.com.ticpass.pos.CategoryStash
import br.com.ticpass.pos.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryHelper: CategoryHelper
) : ViewModel() {

    private val TAG = CategoryViewModel::class.java.simpleName

    private var stash: CategoryStash = mutableMapOf(
        Category.Type.APPLICATION to emptyList(),
        Category.Type.GAME to emptyList()
    )

    val liveData = MutableLiveData<ViewState>()

    private fun contract(): CategoryContract {
        return categoryHelper
    }

    fun getCategoryList(type: Category.Type) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = getCategories(type)

            if (categories.isNotEmpty()) {
                liveData.postValue(ViewState.Success(stash))
                return@launch
            }

            try {
                stash[type] = contract().getAllCategories(type)
                liveData.postValue(ViewState.Success(stash))
            } catch (exception: Exception) {
                Log.e(TAG, "Failed fetching list of categories", exception)
            }
        }
    }

    private fun getCategories(type: Category.Type): List<Category> {
        return stash.getOrPut(type) {
            mutableListOf()
        }
    }
}
