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

package br.com.ticpass.pos.viewmodel.topchart

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.TopChartsContract
import com.aurora.gplayapi.helpers.web.WebTopChartsHelper
import br.com.ticpass.pos.TopChartStash
import br.com.ticpass.pos.data.model.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class TopChartViewModel @Inject constructor(
    private val webTopChartsHelper: WebTopChartsHelper
): ViewModel() {

    private var stash: TopChartStash = mutableMapOf()

    val liveData: MutableLiveData<ViewState> = MutableLiveData()

    private val topChartsContract: TopChartsContract
        get() = webTopChartsHelper

    fun getStreamCluster(type: TopChartsContract.Type, chart: TopChartsContract.Chart) {
        viewModelScope.launch(Dispatchers.IO) {
            if (targetCluster(type, chart).clusterAppList.isNotEmpty()) {
                liveData.postValue(ViewState.Success(stash))
            }

            try {
                val cluster = topChartsContract.getCluster(type.value, chart.value)
                updateCluster(type, chart, cluster)
                liveData.postValue(ViewState.Success(stash))
            } catch (_: Exception) {
            }
        }
    }

    fun nextCluster(type: TopChartsContract.Type, chart: TopChartsContract.Chart) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val target = targetCluster(type, chart)
                    if (target.hasNext()) {
                        val newCluster = topChartsContract.getNextStreamCluster(
                            target.clusterNextPageUrl
                        )

                        updateCluster(type, chart, newCluster)

                        liveData.postValue(ViewState.Success(stash))
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun updateCluster(
        type: TopChartsContract.Type,
        chart: TopChartsContract.Chart,
        newCluster: StreamCluster
    ) {
        val streamCluster = targetCluster(type, chart)
        val mergedCluster = streamCluster.copy(
            clusterNextPageUrl = newCluster.clusterNextPageUrl,
            clusterAppList = streamCluster.clusterAppList + newCluster.clusterAppList
        )

        stash[type]?.set(chart, mergedCluster)
    }

    private fun targetCluster(
        type: TopChartsContract.Type,
        chart: TopChartsContract.Chart
    ): StreamCluster {
        val cluster = stash
            .getOrPut(type) { mutableMapOf() }
            .getOrPut(chart) { StreamCluster() }
        return cluster
    }
}
