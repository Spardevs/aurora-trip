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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import br.com.ticpass.pos.data.model.ViewState
import br.com.ticpass.pos.data.model.ViewState.Loading.getDataAs
import br.com.ticpass.pos.databinding.FragmentGenericWithToolbarBinding
import br.com.ticpass.pos.view.custom.recycler.EndlessRecyclerOnScrollListener
import br.com.ticpass.pos.view.epoxy.controller.CategoryCarouselController
import br.com.ticpass.pos.view.epoxy.controller.GenericCarouselController
import br.com.ticpass.pos.viewmodel.subcategory.CategoryStreamViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryBrowseFragment : BaseFragment<FragmentGenericWithToolbarBinding>(),
    GenericCarouselController.Callbacks {
    private val args: CategoryBrowseFragmentArgs by navArgs()
    private val viewModel: CategoryStreamViewModel by activityViewModels()

    private var streamBundle: StreamBundle? = StreamBundle()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val genericCarouselController = CategoryCarouselController(this)

        // Toolbar
        binding.toolbar.apply {
            title = args.title
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        // RecyclerView
        binding.recycler.setController(genericCarouselController)
        binding.recycler.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                viewModel.observe(args.browseUrl)
            }
        })

        viewModel.getStreamBundle(args.browseUrl)
        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Loading -> {
                    genericCarouselController.setData(null)
                }

                is ViewState.Success<*> -> {
                    val stash = it.getDataAs<Map<String, StreamBundle>>()
                    streamBundle = stash[args.browseUrl]

                    genericCarouselController.setData(streamBundle)
                }

                else -> {}
            }
        }
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        openStreamBrowseFragment(streamCluster)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(args.browseUrl, streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName, app)
    }

    override fun onAppLongClick(app: App) {

    }
}
