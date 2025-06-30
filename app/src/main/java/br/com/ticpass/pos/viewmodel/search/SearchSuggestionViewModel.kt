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

package br.com.ticpass.pos.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import br.com.ticpass.pos.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchSuggestionViewModel @Inject constructor(
    private val authProvider: AuthProvider,
    private val searchHelper: SearchHelper,
    private val webSearchHelper: WebSearchHelper
) : ViewModel() {

    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestEntry>>(emptyList())
    val searchSuggestions = _searchSuggestions.asStateFlow()

    private val helper: SearchContract
        get() = if (authProvider.isAnonymous) webSearchHelper else searchHelper

    fun observeStreamBundles(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchSuggestions.value = helper.searchSuggestions(query)
        }
    }
}
