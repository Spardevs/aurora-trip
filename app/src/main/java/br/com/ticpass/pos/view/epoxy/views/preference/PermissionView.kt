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
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.Permission
import br.com.ticpass.pos.databinding.ViewPermissionBinding
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class PermissionView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewPermissionBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun permission(installer: Permission) {
        binding.line1.text = installer.title
        binding.line2.text = installer.subtitle
    }

    @ModelProp
    fun isGranted(granted: Boolean) {
        if (granted) {
            binding.btnAction.isEnabled = false
            binding.btnAction.text = ContextCompat.getString(context, R.string.action_granted)
        } else {
            binding.btnAction.isEnabled = true
            binding.btnAction.text = ContextCompat.getString(context, R.string.action_grant)
        }
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.btnAction.setOnClickListener(onClickListener)
    }
}
