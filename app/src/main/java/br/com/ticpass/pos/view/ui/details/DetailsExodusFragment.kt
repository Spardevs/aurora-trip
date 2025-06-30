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
import br.com.ticpass.Constants
import br.com.ticpass.extensions.browse
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.ExodusTracker
import br.com.ticpass.pos.data.model.Report
import br.com.ticpass.pos.databinding.FragmentGenericWithToolbarBinding
import br.com.ticpass.pos.view.epoxy.views.HeaderViewModel_
import br.com.ticpass.pos.view.epoxy.views.details.ExodusViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.details.DetailsExodusViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailsExodusFragment : BaseFragment<FragmentGenericWithToolbarBinding>() {

    private val viewModel: DetailsExodusViewModel by viewModels()
    private val args: DetailsExodusFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            title = args.displayName
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        updateController(getExodusTrackersFromReport(args.report))
    }

    private fun updateController(reviews: List<ExodusTracker>) {
        binding.recycler.withModels {
            add(
                HeaderViewModel_()
                    .id("header")
                    .title(getString(R.string.exodus_view_report))
                    .browseUrl("browse")
                    .click { _ -> context?.browse(Constants.EXODUS_REPORT_URL + args.report.id) }
            )
            reviews.forEach {
                add(
                    ExodusViewModel_()
                        .id(it.id)
                        .tracker(it)
                        .click { _ ->
                            context?.browse(it.url)
                        }
                )
            }
        }
    }

    private fun getExodusTrackersFromReport(report: Report): List<ExodusTracker> {
        val trackerObjects = report.trackers.map {
            viewModel.exodusTrackers.getJSONObject(it.toString())
        }.toList()

        return trackerObjects.map {
            ExodusTracker(
                id = it.getInt("id"),
                name = it.getString("name"),
                url = it.getString("website"),
                signature = it.getString("code_signature"),
                date = it.getString("creation_date"),
                description = it.getString("description"),
                networkSignature = it.getString("network_signature"),
                documentation = listOf(it.getString("documentation")),
                categories = listOf(it.getString("categories"))
            )
        }.toList()
    }
}
