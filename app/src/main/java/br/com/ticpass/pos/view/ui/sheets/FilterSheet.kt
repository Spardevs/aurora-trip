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

package br.com.ticpass.pos.view.ui.sheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.Filter
import br.com.ticpass.pos.databinding.SheetFilterBinding
import br.com.ticpass.pos.viewmodel.sheets.FilterViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterSheet : BaseDialogSheet<SheetFilterBinding>() {

    private val viewModel: FilterViewModel by viewModels()

    private lateinit var filter: Filter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filter = viewModel.filterProvider.getSavedFilter()

        attachSingleChips()
        attachMultipleChips()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.filterProvider.saveFilter(filter)
    }

    private fun attachSingleChips() {
        binding.filterGfs.apply {
            isChecked = filter.gsfDependentApps
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter = filter.copy(gsfDependentApps = checked)
            }
        }

        binding.filterPaid.apply {
            isChecked = filter.paidApps
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter = filter.copy(paidApps = checked)
            }
        }

        binding.filterAds.apply {
            isChecked = filter.appsWithAds
            setOnCheckedChangeListener { _, checked ->
                isChecked = checked
                filter = filter.copy(appsWithAds = checked)
            }
        }
    }

    private fun attachMultipleChips() {
        val downloadLabels = resources.getStringArray(R.array.filterDownloadsLabels)
        val downloadValues = resources.getStringArray(R.array.filterDownloadsValues)
        val ratingLabels = resources.getStringArray(R.array.filterRatingLabels)
        val ratingValues = resources.getStringArray(R.array.filterRatingValues)

        downloadLabels.forEachIndexed { index, value ->
            val chip = Chip(requireContext())
            chip.id = index
            chip.text = value
            chip.textColors
            chip.isChecked = filter.downloads == downloadValues[index].toInt()
            binding.downloadChips.addView(chip)
        }

        binding.downloadChips.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = filter.copy(downloads = downloadValues[checkedIds[0]].toInt())
        }

        ratingLabels.forEachIndexed { index, value ->
            val chip = Chip(requireContext())
            chip.id = index
            chip.text = value
            chip.isChecked = filter.rating == ratingValues[index].toFloat()
            binding.ratingChips.addView(chip)
        }

        binding.ratingChips.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = filter.copy(rating = ratingValues[checkedIds[0]].toFloat())
        }
    }
}
