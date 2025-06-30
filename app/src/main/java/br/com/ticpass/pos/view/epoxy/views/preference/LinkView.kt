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

package br.com.ticpass.pos.view.epoxy.views.preference

import android.content.Context
import android.util.AttributeSet
import coil3.load
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import br.com.ticpass.extensions.hide
import br.com.ticpass.extensions.show
import br.com.ticpass.pos.data.model.Link
import br.com.ticpass.pos.databinding.ViewLinkBinding
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class LinkView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewLinkBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun link(link: Link) {
        binding.line1.text = link.title
        binding.line2.text = link.subtitle

        if (link.url.startsWith("http") || link.url.startsWith("upi")) {
            binding.line3.hide()
        } else {
            binding.line3.show()
            binding.line3.text = link.url
        }

        binding.imgIcon.load(link.icon)
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }
}
