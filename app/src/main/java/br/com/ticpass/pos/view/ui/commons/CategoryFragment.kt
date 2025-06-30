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
import br.com.ticpass.Constants
import com.aurora.gplayapi.data.models.Category
import br.com.ticpass.pos.CategoryStash
import br.com.ticpass.pos.data.model.ViewState
import br.com.ticpass.pos.data.model.ViewState.Empty.getDataAs
import br.com.ticpass.pos.databinding.FragmentGenericRecyclerBinding
import br.com.ticpass.pos.view.epoxy.views.CategoryViewModel_
import br.com.ticpass.pos.view.epoxy.views.shimmer.AppListViewShimmerModel_
import br.com.ticpass.pos.viewmodel.category.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryFragment : BaseFragment<FragmentGenericRecyclerBinding>() {
    private val viewModel: CategoryViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(pageType: Int): CategoryFragment {
            return CategoryFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.PAGE_TYPE, pageType)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var pageType = 0
        val bundle = arguments
        if (bundle != null) {
            pageType = bundle.getInt(Constants.PAGE_TYPE, 0)
        }

        when (pageType) {
            0 -> viewModel.getCategoryList(Category.Type.APPLICATION)
            1 -> viewModel.getCategoryList(Category.Type.GAME)
        }

        val categoryType = when (pageType) {
            1 -> Category.Type.GAME
            else -> Category.Type.APPLICATION
        }

        viewModel.liveData.observe(viewLifecycleOwner) {
            when (it) {
                is ViewState.Success<*> -> {
                    val stash = it.getDataAs<CategoryStash>()
                    updateController(stash[categoryType])
                }

                else -> {
                    updateController(emptyList())
                }
            }
        }
    }

    fun updateController(categories: List<Category>?) {
        binding.recycler.withModels {
            if (categories == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                setFilterDuplicates(true)
                categories.forEach {
                    add(
                        CategoryViewModel_()
                            .id(it.title)
                            .category(it)
                            .click { _ -> openCategoryBrowseFragment(it) }
                    )
                }
            }
        }
    }
}
