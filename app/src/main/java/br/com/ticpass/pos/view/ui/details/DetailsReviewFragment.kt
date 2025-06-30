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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.ReviewCluster
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.FragmentDetailsReviewBinding
import br.com.ticpass.pos.view.custom.recycler.EndlessRecyclerOnScrollListener
import br.com.ticpass.pos.view.epoxy.views.AppProgressViewModel_
import br.com.ticpass.pos.view.epoxy.views.details.ReviewViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.review.ReviewViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailsReviewFragment : BaseFragment<FragmentDetailsReviewBinding>() {

    private val args: DetailsReviewFragmentArgs by navArgs()
    private val viewModel: ReviewViewModel by viewModels()

    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener
    private lateinit var filter: Review.Filter
    private lateinit var reviewCluster: ReviewCluster

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust layout for edgeToEdge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.recycler) { layout, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            layout.setPadding(0, 0, 0, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // Toolbar
        binding.toolbar.apply {
            title = args.displayName
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        viewModel.liveData.observe(viewLifecycleOwner) {
            if (!::reviewCluster.isInitialized) {
                endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
                    override fun onLoadMore(currentPage: Int) {
                        if (::reviewCluster.isInitialized) {
                            viewModel.next(reviewCluster.nextPageUrl)
                        }
                    }
                }
                binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
            }

            it?.let {
                reviewCluster = it
                updateController(reviewCluster)
            }
        }

        // Fetch Reviews
        filter = Review.Filter.ALL
        viewModel.fetchReview(args.packageName, filter)

        // Chips
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds[0]) {
                R.id.filter_review_all -> filter = Review.Filter.ALL
                R.id.filter_newest_first -> filter = Review.Filter.NEWEST
                R.id.filter_review_critical -> filter = Review.Filter.CRITICAL
                R.id.filter_review_positive -> filter = Review.Filter.POSITIVE
                R.id.filter_review_five -> filter = Review.Filter.FIVE
                R.id.filter_review_four -> filter = Review.Filter.FOUR
                R.id.filter_review_three -> filter = Review.Filter.THREE
                R.id.filter_review_two -> filter = Review.Filter.TWO
                R.id.filter_review_one -> filter = Review.Filter.ONE
            }

            endlessRecyclerOnScrollListener.resetPageCount()
            viewModel.fetchReview(args.packageName, filter)
        }
    }

    private fun updateController(reviewCluster: ReviewCluster) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            reviewCluster.reviewList.forEach {
                add(
                    ReviewViewModel_()
                        .id(it.commentId)
                        .review(it)
                )
            }

            if (reviewCluster.hasNext()) {
                add(
                    AppProgressViewModel_()
                        .id("progress")
                )
            }
        }
    }
}
