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
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.aurora.gplayapi.data.models.Artwork
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.FragmentScreenshotBinding
import br.com.ticpass.pos.view.epoxy.views.details.LargeScreenshotViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScreenshotFragment : BaseFragment<FragmentScreenshotBinding>() {

    private val args: ScreenshotFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            elevation = 0f
            navigationIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_arrow_back)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        // Recycler View
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
            PagerSnapHelper().attachToRecyclerView(this)
        }

        updateController(args.arrayOfArtwork, args.position)
    }

    private fun updateController(artworks: Array<Artwork>, position: Int) {
        binding.recyclerView.withModels {
            artworks.forEach {
                add(
                    LargeScreenshotViewModel_()
                        .id(it.url)
                        .artwork(it)
                )
            }
            binding.recyclerView.scrollToPosition(position)
        }
    }
}
