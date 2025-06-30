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

package br.com.ticpass.pos.viewmodel.details

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.gplayapi.helpers.contracts.StreamContract
import br.com.ticpass.pos.AppStreamStash
import br.com.ticpass.pos.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class DetailsClusterViewModel @Inject constructor(
    private val appDetailsHelper: AppDetailsHelper,
    private val streamHelper: StreamHelper
) : ViewModel() {

    private val TAG = DetailsClusterViewModel::class.java.simpleName

    val liveData: MutableLiveData<ViewState> = MutableLiveData()
    private val stash: AppStreamStash = mutableMapOf()

    lateinit var type: StreamContract.Type
    lateinit var category: StreamContract.Category

    fun getStreamBundle(streamUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                val bundle = targetBundle(streamUrl)
                if (bundle.streamClusters.isNotEmpty()) {
                    liveData.postValue(ViewState.Success(stash))
                }

                try {
                    if (!bundle.hasCluster() || bundle.hasNext()) {
                        val newBundle = appDetailsHelper.getDetailsStream(streamUrl)

                        val mergedBundle = bundle.copy(
                            streamClusters = bundle.streamClusters + newBundle.streamClusters,
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        )
                        stash[streamUrl] = mergedBundle

                        liveData.postValue(ViewState.Success(stash))
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    fun observeCluster(url: String, streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster =
                            streamHelper.getNextStreamCluster(streamCluster.clusterNextPageUrl)
                        updateCluster(url, streamCluster.id, newCluster)
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

    private fun updateCluster(
        url: String,
        clusterID: Int,
        newCluster: StreamCluster
    ) {
        val bundle = targetBundle(url)
        bundle.streamClusters[clusterID]?.let { oldCluster ->
            val mergedCluster = oldCluster.copy(
                clusterNextPageUrl = newCluster.clusterNextPageUrl,
                clusterAppList = oldCluster.clusterAppList + newCluster.clusterAppList
            )

            val newStreamClusters = bundle.streamClusters.toMutableMap().apply {
                this[clusterID] = mergedCluster
            }

            stash.put(url, bundle.copy(streamClusters = newStreamClusters))
        }
    }

    private fun targetBundle(url: String): StreamBundle {
        val streamBundle = stash.getOrPut(url) {
            StreamBundle()
        }

        return streamBundle
    }
}
