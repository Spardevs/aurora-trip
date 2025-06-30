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
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.data.models.PlayFile
import br.com.ticpass.pos.databinding.ViewFileBinding
import br.com.ticpass.pos.util.CommonUtil
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class FileView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewFileBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun file(file: PlayFile) {
        binding.line1.text = file.name
        binding.line2.text = CommonUtil.addSiPrefix(file.size)
    }
}
