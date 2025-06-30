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
import coil3.load
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.data.models.details.Badge
import br.com.ticpass.pos.databinding.ViewBadgeBinding
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class BadgeView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewBadgeBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun badge(badge: Badge) {
        if (badge.textMajor.isEmpty()) {
            if (badge.textMinor.isEmpty()) {
                binding.txt.text = badge.textDescription
            } else {
                binding.txt.text = badge.textMinor
            }
        } else {
            binding.txt.text = badge.textMajor
        }

        badge.artwork?.let {
            binding.img.load(it.url)
        }
    }
}
