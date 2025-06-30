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

package br.com.ticpass.pos.view.ui.downloads

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import br.com.ticpass.Constants.GITLAB_URL
import br.com.ticpass.extensions.browse
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.download.Download
import br.com.ticpass.pos.databinding.FragmentDownloadBinding
import br.com.ticpass.pos.view.epoxy.views.DownloadViewModel_
import br.com.ticpass.pos.view.epoxy.views.TextDividerViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.NoAppViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.downloads.DownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class DownloadFragment : BaseFragment<FragmentDownloadBinding>() {

    private val viewModel: DownloadViewModel by viewModels()

    private lateinit var downloadList: List<Download>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_force_clear_all -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            viewModel.downloadHelper.clearAllDownloads()
                        }
                    }

                    R.id.action_cancel_all -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            viewModel.downloadHelper.cancelAll()
                        }
                    }

                    R.id.action_clear_completed -> {
                        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
                            viewModel.downloadHelper.clearFinishedDownloads()
                        }
                    }
                }
                true
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadHelper.downloadsList.collectLatest {
                downloadList = it
                updateController(it.reversed())
            }
        }
    }

    private fun updateController(downloads: List<Download>) {
        binding.recycler.withModels {
            if (downloads.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_downloads")
                        .message(R.string.download_none)
                )
            } else {
                downloads.groupBy {
                    DateUtils.getRelativeTimeSpanString(
                        it.downloadedAt,
                        Date().time,
                        DateUtils.DAY_IN_MILLIS
                    ).toString()
                }.forEach { (date, downloadList) ->
                    add(
                        TextDividerViewModel_()
                            .id(date)
                            .title(date)
                    )

                    downloadList.forEach {
                        add(
                            DownloadViewModel_()
                                .id(it.packageName)
                                .download(it)
                                .click { _ ->
                                    if (it.packageName == requireContext().packageName) {
                                        requireContext().browse(GITLAB_URL)
                                    } else {
                                        openDetailsFragment(it.packageName)
                                    }
                                }
                                .longClick { _ ->
                                    openDownloadMenuSheet(it.packageName)
                                    true
                                }
                        )
                    }
                }
            }
        }
    }

    private fun openDownloadMenuSheet(packageName: String) {
        val download = downloadList.find { it.packageName == packageName }!!
        findNavController().navigate(
            DownloadFragmentDirections.actionDownloadFragmentToDownloadMenuSheet(download)
        )
    }
}
