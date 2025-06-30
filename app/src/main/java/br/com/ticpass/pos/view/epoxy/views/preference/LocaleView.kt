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
import android.widget.CompoundButton
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import br.com.ticpass.pos.databinding.ViewLocaleBinding
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView
import java.util.Locale

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class LocaleView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewLocaleBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun locale(locale: Locale) {
        binding.line1.text = locale.displayName
        binding.line2.text = locale.getDisplayLanguage(locale)
    }

    @ModelProp
    fun markChecked(isChecked: Boolean) {
        binding.checkbox.isChecked = isChecked
        binding.checkbox.isEnabled = !isChecked
    }

    @CallbackProp
    fun checked(onCheckedChangeListener: CompoundButton.OnCheckedChangeListener?) {
        binding.checkbox.setOnCheckedChangeListener(onCheckedChangeListener)
    }
}
