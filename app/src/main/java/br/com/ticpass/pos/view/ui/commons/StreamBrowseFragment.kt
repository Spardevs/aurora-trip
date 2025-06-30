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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.StreamCluster
import br.com.ticpass.pos.databinding.FragmentGenericWithToolbarBinding
import br.com.ticpass.pos.view.custom.recycler.EndlessRecyclerOnScrollListener
import br.com.ticpass.pos.view.epoxy.views.AppProgressViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.AppListViewModel_
import br.com.ticpass.pos.view.epoxy.views.shimmer.AppListViewShimmerModel_
import br.com.ticpass.pos.viewmodel.browse.StreamBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StreamBrowseFragment : BaseFragment<FragmentGenericWithToolbarBinding>() {
    private val args: StreamBrowseFragmentArgs by navArgs()
    private val viewModel: StreamBrowseViewModel by viewModels()

    private lateinit var streamCluster: StreamCluster

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        streamCluster = args.cluster

        // Toolbar
        binding.toolbar.apply {
            title = streamCluster.clusterTitle
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        binding.recycler.addOnScrollListener(object :
            EndlessRecyclerOnScrollListener(visibleThreshold = 4) {
            override fun onLoadMore(currentPage: Int) {
                viewModel.nextCluster()
            }
        })

        viewModel.seedCluster(streamCluster)
        viewModel.liveData.observe(viewLifecycleOwner) {
            updateController(it)
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
                streamCluster.clusterAppList.forEach {
                    add(
                        AppListViewModel_()
                            .id(it.packageName.hashCode())
                            .app(it)
                            .click { _ -> openDetailsFragment(it.packageName, it) }
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
