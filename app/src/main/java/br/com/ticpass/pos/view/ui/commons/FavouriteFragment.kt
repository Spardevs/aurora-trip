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

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import br.com.ticpass.Constants
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.favourite.Favourite
import br.com.ticpass.pos.data.room.favourite.Favourite.Companion.toApp
import br.com.ticpass.pos.databinding.FragmentFavouriteBinding
import br.com.ticpass.pos.view.epoxy.views.FavouriteViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.NoAppViewModel_
import br.com.ticpass.pos.view.epoxy.views.shimmer.AppListViewShimmerModel_
import br.com.ticpass.pos.viewmodel.all.FavouriteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class FavouriteFragment : BaseFragment<FragmentFavouriteBinding>() {
    private val viewModel: FavouriteViewModel by viewModels()

    private val startForDocumentImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) importFavourites(it) else toast(R.string.toast_fav_import_failed)
        }
    private val startForDocumentExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument(Constants.JSON_MIME_TYPE)) {
            if (it != null) exportFavourites(it) else toast(R.string.toast_fav_export_failed)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favouritesList.collect {
                updateController(it)
            }
        }

        // Toolbar
        binding.toolbar.apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_import -> startForDocumentImport.launch(arrayOf(Constants.JSON_MIME_TYPE))
                    R.id.action_export -> {
                        startForDocumentExport.launch(
                            "aurora_store_favourites_${Calendar.getInstance().time.time}.json"
                        )
                    }

                    else -> {}
                }
                true
            }
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }

    private fun updateController(favourites: List<Favourite>?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (favourites == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else if (favourites.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_app")
                        .icon(R.drawable.ic_favorite_unchecked)
                        .message(R.string.details_no_favourites)
                )
            } else {
                favourites.forEach {
                    add(
                        FavouriteViewModel_()
                            .id(it.packageName.hashCode())
                            .favourite(it)
                            .onClick { _ -> openDetailsFragment(it.packageName, it.toApp()) }
                            .onFavourite { _ -> viewModel.removeFavourite(it.packageName) }
                    )
                }
            }
        }
    }

    private fun importFavourites(uri: Uri) {
        viewModel.importFavourites(requireContext(), uri)
        binding.recycler.requestModelBuild()
        toast(R.string.toast_fav_import_success)
    }

    private fun exportFavourites(uri: Uri) {
        viewModel.exportFavourites(requireContext(), uri)
        toast(R.string.toast_fav_export_success)
    }
}
