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

package br.com.ticpass.pos.viewmodel.review

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.ReviewCluster
import com.aurora.gplayapi.helpers.ReviewsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewsHelper: ReviewsHelper
) : ViewModel() {
    val TAG = javaClass.simpleName

    val liveData: MutableLiveData<ReviewCluster> = MutableLiveData()

    private lateinit var reviewsCluster: ReviewCluster

    fun fetchReview(packageName: String, filter: Review.Filter) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reviewsCluster = reviewsHelper.getReviews(packageName, filter)
                liveData.postValue(reviewsCluster)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch reviews", e)
            }
        }
    }

    fun next(nextReviewPageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentCluster = reviewsCluster
                val nextReviewCluster = reviewsHelper.next(nextReviewPageUrl)

                reviewsCluster = currentCluster.copy(
                    nextPageUrl = nextReviewCluster.nextPageUrl,
                    reviewList = currentCluster.reviewList + nextReviewCluster.reviewList
                )

                liveData.postValue(reviewsCluster)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch next reviews $nextReviewPageUrl", e)
            }
        }
    }
}
