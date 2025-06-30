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
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewInfoBinding
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class InfoView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewInfoBinding>(context, attrs, defStyleAttr) {

    @ModelProp(options = [ModelProp.Option.IgnoreRequireHashCode])
    fun badge(info: Map.Entry<String, String>) {
        binding.txtTitle.text = when (info.key) {
            "DOWNLOAD" -> ContextCompat.getString(context, R.string.app_info_downloads)
            "UPDATED_ON" -> ContextCompat.getString(context, R.string.app_info_updated_on)
            "REQUIRES" -> ContextCompat.getString(context, R.string.app_info_min_android)
            "TARGET" -> ContextCompat.getString(context, R.string.app_info_target_android)
            else -> info.key
        }

        binding.txtSubtitle.text = info.value
    }

    @OnViewRecycled
    fun clear() {
        binding.txtTitle.text = null
        binding.txtSubtitle.text = null
    }
}
