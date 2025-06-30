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

package br.com.ticpass.pos.view.epoxy.views

import android.content.Context
import android.util.AttributeSet
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import br.com.ticpass.pos.databinding.ViewHeaderUpdateBinding


@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class UpdateHeaderView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewHeaderUpdateBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun title(title: String) {
        binding.txtTitle.text = title
    }

    @ModelProp
    fun action(action: String) {
        binding.btnAction.text = action
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.btnAction.setOnClickListener(onClickListener)
    }

    @OnViewRecycled
    fun clear() {
        binding.btnAction.isEnabled = true
    }
}
