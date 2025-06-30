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

package br.com.ticpass.pos.viewmodel.browse

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.ExpandedBrowseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class ExpandedStreamBrowseViewModel @Inject constructor(
    private val streamHelper: ExpandedBrowseHelper
) : ViewModel() {

    private val TAG = ExpandedStreamBrowseViewModel::class.java.simpleName

    val liveData: MutableLiveData<StreamCluster> = MutableLiveData()
    var streamCluster: StreamCluster = StreamCluster()

    fun getInitialCluster(expandedStreamUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val browseResponse = streamHelper.getBrowseStreamResponse(expandedStreamUrl)
                    if (browseResponse.hasBrowseTab()) {
                        streamCluster =
                            streamHelper.getExpandedBrowseClusters(browseResponse.browseTab.listUrl)
                        liveData.postValue(streamCluster)
                    } else {
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun next() {
        Log.e(TAG, "NEXT CALED")
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val newCluster = streamHelper.getExpandedBrowseClusters(
                        streamCluster.clusterNextPageUrl
                    )

                    streamCluster = streamCluster.copy(
                        clusterAppList = streamCluster.clusterAppList + newCluster.clusterAppList,
                        clusterNextPageUrl = newCluster.clusterNextPageUrl
                    )

                    liveData.postValue(streamCluster)

                    if (!streamCluster.hasNext()) {
                        Log.i(TAG, "End of Bundle")
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to fetch next stream", exception)
                }
            }
        }
    }
}
