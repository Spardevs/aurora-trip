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
import coil3.load
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.data.models.details.DevStream
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.ViewState
import br.com.ticpass.pos.databinding.FragmentDevProfileBinding
import br.com.ticpass.pos.view.epoxy.controller.DeveloperCarouselController
import br.com.ticpass.pos.view.epoxy.controller.GenericCarouselController
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.details.DevProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevProfileFragment : BaseFragment<FragmentDevProfileBinding>(),
    GenericCarouselController.Callbacks {

    private val args: DevProfileFragmentArgs by navArgs()
    private val viewModel: DevProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val developerCarouselController = DeveloperCarouselController(this)

        // Toolbar
        binding.toolbar.apply {
            title = if (args.title.isNullOrBlank()) getString(R.string.details_dev_profile) else args.title
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        // RecyclerView
        binding.recycler.setController(developerCarouselController)

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Empty -> {
                }

                is ViewState.Loading -> {

                }

                is ViewState.Error -> {

                }

                is ViewState.Status -> {

                }

                is ViewState.Success<*> -> {
                    (it.data as DevStream).apply {
                        binding.toolbar.title = title
                        binding.txtDevName.text = title
                        binding.txtDevDescription.text = description
                        binding.imgIcon.load(imgUrl)
                        binding.viewFlipper.displayedChild = 0
                        developerCarouselController.setData(streamBundle)
                    }
                }
            }
        }

        binding.viewFlipper.displayedChild = 1
        viewModel.getStreamBundle(args.devId)
    }

    override fun onHeaderClicked(streamCluster: StreamCluster) {
        openStreamBrowseFragment(streamCluster.clusterBrowseUrl, streamCluster.clusterTitle)
    }

    override fun onClusterScrolled(streamCluster: StreamCluster) {
        viewModel.observeCluster(streamCluster)
    }

    override fun onAppClick(app: App) {
        openDetailsFragment(app.packageName, app)
    }

    override fun onAppLongClick(app: App) {

    }
}
