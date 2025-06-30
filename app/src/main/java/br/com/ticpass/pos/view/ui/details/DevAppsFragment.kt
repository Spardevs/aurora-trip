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

package br.com.ticpass.pos.view.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import br.com.ticpass.pos.AppStreamStash
import br.com.ticpass.pos.data.model.ViewState
import br.com.ticpass.pos.data.model.ViewState.Loading.getDataAs
import br.com.ticpass.pos.databinding.FragmentGenericWithToolbarBinding
import br.com.ticpass.pos.view.custom.recycler.EndlessRecyclerOnScrollListener
import br.com.ticpass.pos.view.epoxy.controller.GenericCarouselController
import br.com.ticpass.pos.view.epoxy.controller.SearchCarouselController
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.search.SearchResultViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevAppsFragment : BaseFragment<FragmentGenericWithToolbarBinding>(),
    GenericCarouselController.Callbacks {

    private val args: DevAppsFragmentArgs by navArgs()

    private val viewModel: SearchResultViewModel by viewModels()
    private val controller = SearchCarouselController(this)

    private var query: String = ""
        get() = "pub:${args.developerName}"

    private var scrollListener: EndlessRecyclerOnScrollListener =
        object : EndlessRecyclerOnScrollListener(visibleThreshold = 4) {
            override fun onLoadMore(currentPage: Int) {
                viewModel.observe(query)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.apply {
                title = args.developerName
                setNavigationOnClickListener { findNavController().navigateUp() }
            }

            recycler.setController(controller)
            recycler.addOnScrollListener(scrollListener)
        }

        with(viewModel) {
            search(query)

            liveData.observe(viewLifecycleOwner) {
                when (it) {
                    is ViewState.Loading -> {
                        controller.setData(null)
                    }

                    is ViewState.Success<*> -> {
                        val stash = it.getDataAs<AppStreamStash>()
                        controller.setData(stash[query])
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        openStreamBrowseFragment(streamCluster)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(query, streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName, app)
    }

    override fun onAppLongClick(app: App) {

    }
}
