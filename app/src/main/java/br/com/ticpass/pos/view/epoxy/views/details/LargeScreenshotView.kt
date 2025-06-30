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

package br.com.ticpass.pos.view.epoxy.views.details

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import coil3.load
import coil3.request.placeholder
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.data.models.Artwork
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewScreenshotLargeBinding
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT,
    baseModelClass = BaseModel::class
)
class LargeScreenshotView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewScreenshotLargeBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun artwork(artwork: Artwork) {
        val displayMetrics = Resources.getSystem().displayMetrics
        binding.img.load("${artwork.url}=rw-w${displayMetrics.widthPixels}-v1-e15") {
            placeholder(R.drawable.bg_placeholder)
        }
    }
}
