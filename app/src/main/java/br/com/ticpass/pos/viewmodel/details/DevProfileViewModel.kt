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
import com.aurora.gplayapi.data.models.details.DevStream
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.gplayapi.helpers.contracts.StreamContract
import br.com.ticpass.pos.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class DevProfileViewModel @Inject constructor(
    private val appDetailsHelper: AppDetailsHelper,
    private val streamHelper: StreamHelper
) : ViewModel() {

    private val TAG = DevProfileViewModel::class.java.simpleName

    val liveData: MutableLiveData<ViewState> = MutableLiveData()
    var devStream:DevStream = DevStream()
    var streamBundle: StreamBundle = StreamBundle()

    lateinit var type: StreamContract.Type
    lateinit var category: StreamContract.Category

    fun getStreamBundle(devId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    devStream = appDetailsHelper.getDeveloperStream(devId)
                    streamBundle = devStream.streamBundle
                    liveData.postValue(ViewState.Success(devStream))
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    fun observeCluster(streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster = streamHelper.getNextStreamCluster(streamCluster.clusterNextPageUrl)
                        updateCluster(newCluster)
                        devStream = devStream.copy(streamBundle = streamBundle)
                        liveData.postValue(ViewState.Success(devStream))
                    } else {
                        Log.i(TAG, "End of cluster")
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    private fun updateCluster(newCluster: StreamCluster) {
        streamBundle.streamClusters[newCluster.id]?.let { oldCluster ->
            val mergedCluster = oldCluster.copy(
                clusterNextPageUrl = newCluster.clusterNextPageUrl,
                clusterAppList = oldCluster.clusterAppList + newCluster.clusterAppList
            )

            val newStreamClusters = streamBundle.streamClusters.toMutableMap().apply {
                this[newCluster.id] = mergedCluster
            }

            streamBundle = streamBundle.copy(streamClusters = newStreamClusters)
        }
    }
}
