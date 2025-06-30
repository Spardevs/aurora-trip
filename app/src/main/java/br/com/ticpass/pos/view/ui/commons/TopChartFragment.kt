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

package br.com.ticpass.pos.view.ui.commons

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import br.com.ticpass.Constants
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.TopChartsContract.Chart
import com.aurora.gplayapi.helpers.contracts.TopChartsContract.Type
import br.com.ticpass.pos.TopChartStash
import br.com.ticpass.pos.data.model.ViewState
import br.com.ticpass.pos.data.model.ViewState.Empty.getDataAs
import br.com.ticpass.pos.databinding.FragmentTopContainerBinding
import br.com.ticpass.pos.view.custom.recycler.EndlessRecyclerOnScrollListener
import br.com.ticpass.pos.view.epoxy.views.AppProgressViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.AppListViewModel_
import br.com.ticpass.pos.view.epoxy.views.shimmer.AppListViewShimmerModel_
import br.com.ticpass.pos.viewmodel.topchart.TopChartViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopChartFragment : BaseFragment<FragmentTopContainerBinding>() {

    private val viewModel: TopChartViewModel by activityViewModels()

    private var streamCluster: StreamCluster? = StreamCluster()

    companion object {
        @JvmStatic
        fun newInstance(chartType: Int, chartCategory: Int): TopChartFragment {
            return TopChartFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.TOP_CHART_TYPE, chartType)
                    putInt(Constants.TOP_CHART_CATEGORY, chartCategory)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var type = 0
        var category = 0
        val bundle = arguments

        if (bundle != null) {
            type = bundle.getInt(Constants.TOP_CHART_TYPE, 0)
            category = bundle.getInt(Constants.TOP_CHART_CATEGORY, 0)
        }

        val chartType = when (type) {
            1 -> Type.GAME
            else -> Type.APPLICATION
        }

        val chartCategory = when (category) {
            1 -> Chart.TOP_GROSSING
            2 -> Chart.MOVERS_SHAKERS
            3 -> Chart.TOP_SELLING_PAID
            else -> Chart.TOP_SELLING_FREE
        }

        binding.recycler.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                viewModel.nextCluster(chartType, chartCategory)
            }
        })

        updateController(null)

        viewModel.getStreamCluster(chartType, chartCategory)
        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Loading, is ViewState.Error -> {
                    updateController(null)
                }

                is ViewState.Success<*> -> {
                    val stash = it.getDataAs<TopChartStash>()
                    streamCluster = stash[chartType]?.get(chartCategory)

                    updateController(streamCluster)
                }

                else -> {}
            }
        }
    }

    private fun updateController(streamCluster: StreamCluster?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (streamCluster == null) {
                for (i in 1..6) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                streamCluster.clusterAppList.forEach { app ->
                    add(
                        AppListViewModel_()
                            .id(app.id)
                            .app(app)
                            .click { _ -> openDetailsFragment(app.packageName, app) }
                    )
                }

                if (streamCluster.hasNext()) {
                    add(
                        AppProgressViewModel_()
                            .id("progress")
                    )
                }
            }
        }
    }
}
