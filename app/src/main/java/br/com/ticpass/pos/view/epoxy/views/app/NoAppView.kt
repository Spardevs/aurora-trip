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

package br.com.ticpass.pos.view.epoxy.views.app

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import coil3.load
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import br.com.ticpass.pos.databinding.ViewNoAppBinding
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT,
    baseModelClass = BaseModel::class
)
class NoAppView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewNoAppBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun message(@StringRes message: Int) {
        binding.txt.text = context.getString(message)
    }

    @ModelProp
    fun icon(@DrawableRes icon: Int) {
        binding.img.load(icon)
    }

    @JvmOverloads
    @ModelProp
    fun showAction(visibility: Boolean = false) {
        binding.button.isVisible = visibility
    }

    @JvmOverloads
    @ModelProp
    fun actionMessage(@StringRes message: Int? = null) {
        message?.let { binding.button.text = context.getString(message) }
    }

    @JvmOverloads
    @CallbackProp
    fun actionCallback(viewOnClickListener: OnClickListener? = null) {
        binding.button.setOnClickListener(viewOnClickListener)
    }
}
