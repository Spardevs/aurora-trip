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

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import br.com.ticpass.pos.AppStreamStash
import br.com.ticpass.pos.data.model.ViewState
import br.com.ticpass.pos.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val authProvider: AuthProvider,
    private val searchHelper: SearchHelper,
    private val webSearchHelper: WebSearchHelper
) : ViewModel() {

    private val TAG = SearchResultViewModel::class.java.simpleName

    val liveData: MutableLiveData<ViewState> = MutableLiveData()

    private val stash: AppStreamStash = mutableMapOf()

    private val contract: SearchContract
        get() = if (authProvider.isAnonymous) webSearchHelper else searchHelper

    private val stashMutex = Mutex()

    @Synchronized
    fun search(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stashMutex.withLock {
                    liveData.postValue(ViewState.Loading)

                    var bundle = targetBundle(query)

                    // Post existing data if any clusters exist
                    if (bundle.hasCluster()) {
                        liveData.postValue(ViewState.Success(stash.toMap()))
                        return@launch
                    }

                    // Fetch new stream bundle
                    val newBundle = contract.searchResults(query)

                    bundle = bundle.copy(
                        streamClusters = newBundle.streamClusters,
                        streamNextPageUrl = newBundle.streamNextPageUrl
                    )

                    stash[query] = bundle

                    liveData.postValue(ViewState.Success(stash.toMap()))
                }
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    fun observe(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stashMutex.withLock {
                    var bundle = targetBundle(query)

                    if (bundle.hasNext()) {
                        val newBundle = contract.nextStreamBundle(
                            query,
                            bundle.streamNextPageUrl
                        )

                        // Update old bundle
                        bundle = bundle.copy(
                            streamClusters = bundle.streamClusters + newBundle.streamClusters,
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        )

                        stash[query] = bundle

                        liveData.postValue(ViewState.Success(stash.toMap()))
                    } else {
                        Log.i(TAG, "End of Bundle")

                        // If stream ends, likely there are clusters that need to be processed
                        bundle.streamClusters.values.forEach {
                            if (it.clusterNextPageUrl.isEmpty()) {
                                postClusterEnd(query, it.id)
                            }

                            // Empty title or query as title indicates main stream cluster
                            if (it.clusterTitle.isEmpty() or (it.clusterTitle == bundle.streamTitle)) {
                                observeCluster(query, it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    fun observeCluster(query: String, streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (streamCluster.hasNext()) {
                    val newCluster = contract.nextStreamCluster(
                        query,
                        streamCluster.clusterNextPageUrl
                    )
                    stashMutex.withLock {
                        updateCluster(query, streamCluster.id, newCluster)
                    }

                    liveData.postValue(ViewState.Success(stash.toMap()))
                } else {
                    stashMutex.withLock {
                        postClusterEnd(query, streamCluster.id)
                    }

                    liveData.postValue(ViewState.Success(stash.toMap()))
                }
            } catch (e: Exception) {
                liveData.postValue(ViewState.Error(e.message))
            }
        }
    }

    private fun updateCluster(query: String, clusterID: Int, newCluster: StreamCluster) {
        val bundle = stash[query] ?: return
        val oldCluster = bundle.streamClusters[clusterID] ?: return

        val mergedCluster = oldCluster.copy(
            clusterNextPageUrl = newCluster.clusterNextPageUrl,
            clusterAppList = oldCluster.clusterAppList + newCluster.clusterAppList
        )

        val updatedClusters = bundle.streamClusters.toMutableMap().apply {
            this[clusterID] = mergedCluster
        }

        stash[query] = bundle.copy(streamClusters = updatedClusters)
    }

    private fun postClusterEnd(query: String, clusterID: Int) {
        val bundle = stash[query] ?: return
        val oldCluster = bundle.streamClusters[clusterID] ?: return

        val updatedCluster = oldCluster.copy(clusterNextPageUrl = "")
        val updatedClusters = bundle.streamClusters.toMutableMap().apply {
            this[clusterID] = updatedCluster
        }

        stash[query] = bundle.copy(streamClusters = updatedClusters)
    }

    private fun targetBundle(query: String): StreamBundle {
        return stash.getOrPut(query.trim()) { StreamBundle(streamTitle = query) }
    }
}
