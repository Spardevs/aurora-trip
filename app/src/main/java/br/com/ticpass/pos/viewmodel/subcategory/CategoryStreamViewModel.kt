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

package br.com.ticpass.pos.viewmodel.subcategory

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.CategoryStreamContract
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.web.WebCategoryStreamHelper
import br.com.ticpass.pos.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class CategoryStreamViewModel @Inject constructor(
    private val webCategoryStreamHelper: WebCategoryStreamHelper
) : ViewModel() {

    private val TAG = CategoryStreamViewModel::class.java.simpleName

    val liveData: MutableLiveData<ViewState> = MutableLiveData()

    private var stash: MutableMap<String, StreamBundle> = mutableMapOf()

    private val categoryStreamContract: CategoryStreamContract
        get() = webCategoryStreamHelper

    fun getStreamBundle(browseUrl: String) {
        liveData.postValue(ViewState.Loading)
        observe(browseUrl)
    }

    fun observe(browseUrl: String) {
        liveData.postValue(ViewState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                val bundle = targetBundle(browseUrl)
                if (bundle.streamClusters.isNotEmpty()) {
                    liveData.postValue(ViewState.Success(stash))
                }

                try {
                    if (!bundle.hasCluster() || bundle.hasNext()) {
                        //Fetch new stream bundle
                        val newBundle = if (bundle.streamClusters.isEmpty()) {
                            categoryStreamContract.fetch(browseUrl)
                        } else {
                            categoryStreamContract.nextStreamBundle(
                                StreamContract.Category.NONE,
                                bundle.streamNextPageUrl
                            )
                        }

                        //Update old bundle
                        val mergedBundle = bundle.copy(
                            streamClusters = bundle.streamClusters + newBundle.streamClusters,
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        )
                        stash[browseUrl] = mergedBundle

                        //Post updated to UI
                        liveData.postValue(ViewState.Success(stash))
                    } else {
                        Log.i(TAG, "End of Bundle")
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    fun observeCluster(browseUrl: String, streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster = categoryStreamContract.nextStreamCluster(
                            streamCluster.clusterNextPageUrl
                        )
                        updateCluster(browseUrl, streamCluster.id, newCluster)
                        liveData.postValue(ViewState.Success(stash))
                    } else {
                        Log.i(TAG, "End of cluster")
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    private fun updateCluster(browseUrl: String, clusterID: Int, newCluster: StreamCluster) {
        val bundle = targetBundle(browseUrl)
        bundle.streamClusters[clusterID]?.let { oldCluster ->
            val mergedCluster = oldCluster.copy(
                clusterNextPageUrl = newCluster.clusterNextPageUrl,
                clusterAppList = oldCluster.clusterAppList + newCluster.clusterAppList
            )

            val newStreamClusters = bundle.streamClusters.toMutableMap().apply {
                this[clusterID] = mergedCluster
            }

            stash.put(browseUrl, bundle.copy(streamClusters = newStreamClusters))
        }
    }

    private fun targetBundle(browseUrl: String): StreamBundle {
        val streamBundle = stash.getOrPut(browseUrl) { StreamBundle() }
        return streamBundle
    }
}
