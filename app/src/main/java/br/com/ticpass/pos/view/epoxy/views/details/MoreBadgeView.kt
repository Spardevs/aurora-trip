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
import android.util.AttributeSet
import androidx.core.text.HtmlCompat
import coil3.load
import coil3.request.placeholder
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.data.models.details.Badge
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewMoreBadgeBinding
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class MoreBadgeView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewMoreBadgeBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun badge(badge: Badge) {
        binding.line1.text = badge.textMajor

        badge.textMinorHtml?.let {
            if (it.isNotEmpty()) {
                binding.line2.text = HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT)
            } else {
                binding.line2.text = badge.textMinor
            }
        }

        badge.textDescription?.let {
            if (it.isNotEmpty()) {
                binding.line2.text = it
            }
        }

        badge.artwork?.let {
            binding.img.load(it.url) {
                placeholder(R.drawable.ic_arrow_right)
            }
        }
    }
}
