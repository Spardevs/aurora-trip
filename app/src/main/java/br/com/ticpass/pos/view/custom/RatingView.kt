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

package br.com.ticpass.pos.view.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewRatingBinding

class RatingView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewRatingBinding

    var number = 0
    var max = 0
    var rating = 0

    constructor(context: Context, number: Int, max: Int, rating: Int) : this(context) {
        this.number = number
        this.max = max
        this.rating = rating
        init(context)
    }

    private fun init(context: Context) {
        val view = inflate(context, R.layout.view_rating, this)
        binding = ViewRatingBinding.bind(view)

        binding.avgNum.text = number.toString()
        binding.avgRating.max = max
        binding.avgRating.progress = rating
    }
}
