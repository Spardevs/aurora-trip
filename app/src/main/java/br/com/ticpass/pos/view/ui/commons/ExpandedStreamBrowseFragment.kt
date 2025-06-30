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
import com.airbnb.epoxy.EpoxyModel
import com.aurora.gplayapi.data.models.StreamCluster
import br.com.ticpass.pos.databinding.FragmentGenericWithToolbarBinding
import br.com.ticpass.pos.view.custom.recycler.EndlessRecyclerOnScrollListener
import br.com.ticpass.pos.view.epoxy.groups.CarouselHorizontalModel_
import br.com.ticpass.pos.view.epoxy.views.AppProgressViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.AppListViewModel_
import br.com.ticpass.pos.view.epoxy.views.details.MiniScreenshotView
import br.com.ticpass.pos.view.epoxy.views.details.MiniScreenshotViewModel_
import br.com.ticpass.pos.view.epoxy.views.shimmer.AppListViewShimmerModel_
import br.com.ticpass.pos.viewmodel.browse.ExpandedStreamBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExpandedStreamBrowseFragment : BaseFragment<FragmentGenericWithToolbarBinding>() {
    private val args: ExpandedStreamBrowseFragmentArgs by navArgs()
    private val viewModel: ExpandedStreamBrowseViewModel by viewModels()

    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener
    private lateinit var cluster: StreamCluster

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            title = args.title
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        viewModel.liveData.observe(viewLifecycleOwner) {
            if (!::cluster.isInitialized) attachRecycler()
            cluster = it

            updateController(cluster)
            updateTitle(cluster)
        }

        viewModel.getInitialCluster(args.expandedStreamUrl)
        updateController(null)
    }

    private fun updateTitle(streamCluster: StreamCluster) {
        if (streamCluster.clusterTitle.isNotEmpty())
            binding.toolbar.title = streamCluster.clusterTitle
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                viewModel.next()
            }
        }
        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
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
                    val screenshotsViewModels = mutableListOf<EpoxyModel<*>>()

                    for ((position, artwork) in it.screenshots.withIndex()) {
                        screenshotsViewModels.add(
                            MiniScreenshotViewModel_()
                                .id(artwork.url)
                                .position(position)
                                .artwork(artwork)
                                .callback(object : MiniScreenshotView.ScreenshotCallback {
                                    override fun onClick(position: Int) {
                                        openScreenshotFragment(it, position)
                                    }
                                })
                        )
                    }

                    if (screenshotsViewModels.isNotEmpty()) {
                        add(
                            CarouselHorizontalModel_()
                                .id("${it.id}_screenshots")
                                .models(screenshotsViewModels)
                        )
                    }

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
