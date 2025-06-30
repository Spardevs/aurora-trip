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
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.MinimalApp
import br.com.ticpass.pos.databinding.ViewPackageBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class InstalledAppView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewPackageBinding>(context, attrs, defStyleAttr) {

    @ModelProp(options = [ModelProp.Option.IgnoreRequireHashCode])
    fun packageInfo(app: MinimalApp) {
        binding.imgIcon.load(app.icon) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(25F))
        }

        binding.txtLine1.text = app.displayName
        binding.txtLine2.text = app.packageName
        binding.txtLine3.text = ("${app.versionName} (${app.versionCode})")
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        binding.root.setOnLongClickListener(onClickListener)
    }
}
