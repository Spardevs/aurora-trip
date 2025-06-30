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
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract.Category
import com.aurora.gplayapi.helpers.contracts.StreamContract.Type
import br.com.ticpass.pos.HomeStash
import br.com.ticpass.pos.data.model.ViewState
import br.com.ticpass.pos.data.model.ViewState.Loading.getDataAs
import br.com.ticpass.pos.databinding.FragmentForYouBinding
import br.com.ticpass.pos.view.custom.recycler.EndlessRecyclerOnScrollListener
import br.com.ticpass.pos.view.epoxy.controller.GenericCarouselController
import br.com.ticpass.pos.viewmodel.homestream.StreamViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForYouFragment : BaseFragment<FragmentForYouBinding>(),
    GenericCarouselController.Callbacks {
    private val viewModel: StreamViewModel by activityViewModels()

    private var category: Category = Category.APPLICATION
    private var streamBundle: StreamBundle? = StreamBundle()

    companion object {
        @JvmStatic
        fun newInstance(pageType: Int): ForYouFragment {
            return ForYouFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.PAGE_TYPE, pageType)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val genericCarouselController = GenericCarouselController(this)

        var pageType = 0
        val bundle = arguments
        if (bundle != null) {
            pageType = bundle.getInt(Constants.PAGE_TYPE, 0)
        }

        category = if (pageType == 0) Category.APPLICATION else Category.GAME

        binding.recycler.setController(genericCarouselController)
        binding.recycler.addOnScrollListener(
            object : EndlessRecyclerOnScrollListener(visibleThreshold = 4) {
                override fun onLoadMore(currentPage: Int) {
                    viewModel.observe(category, Type.HOME)
                }
            }
        )

        viewModel.getStreamBundle(category, Type.HOME)
        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Loading -> {
                    genericCarouselController.setData(null)
                }

                is ViewState.Success<*> -> {
                    val stash = it.getDataAs<HomeStash>()
                    streamBundle = stash[category]

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
        viewModel.observeCluster(category, streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName, app)
    }

    override fun onAppLongClick(app: App) {

    }
}
