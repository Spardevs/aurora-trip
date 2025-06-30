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
import com.aurora.gplayapi.helpers.web.WebStreamHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamBrowseViewModel @Inject constructor(
    private val streamHelper: WebStreamHelper
) : ViewModel() {

    private val TAG = StreamBrowseViewModel::class.java.simpleName

    val liveData: MutableLiveData<StreamCluster> = MutableLiveData()

    private lateinit var streamCluster: StreamCluster

    fun seedCluster(cluster: StreamCluster) {
        streamCluster = cluster
        liveData.postValue(streamCluster)
    }

    fun nextCluster() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (streamCluster.hasNext()) {
                    val next = streamHelper.nextStreamCluster(streamCluster.clusterNextPageUrl)

                    streamCluster = streamCluster.copy(
                        clusterNextPageUrl = next.clusterNextPageUrl,
                        clusterAppList = streamCluster.clusterAppList + next.clusterAppList
                    )

                    liveData.postValue(streamCluster)
                } else {
                    Log.i(TAG, "End of Cluster")
                    postClusterEnd()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun postClusterEnd() {
        streamCluster = streamCluster.copy(
            clusterNextPageUrl = ""
        )
        liveData.postValue(streamCluster)
    }
}
