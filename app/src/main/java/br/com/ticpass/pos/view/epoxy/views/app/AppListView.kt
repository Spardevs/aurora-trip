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
import androidx.core.content.ContextCompat
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.data.models.App
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewAppListBinding
import br.com.ticpass.pos.util.CommonUtil
import br.com.ticpass.pos.view.epoxy.views.BaseModel
import br.com.ticpass.pos.view.epoxy.views.BaseView

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class AppListView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewAppListBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun app(app: App) {
        binding.imgIcon.load(app.iconArtwork.url) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(25F))
        }

        binding.txtLine1.text = app.displayName
        binding.txtLine2.text = app.developerName

        val extras: MutableList<String> = mutableListOf()
        extras.add(if (app.size > 0) CommonUtil.addSiPrefix(app.size) else app.downloadString)
        extras.add("${app.labeledRating}★")
        extras.add(
            if (app.isFree)
                ContextCompat.getString(context, R.string.details_free)
            else
                ContextCompat.getString(context, R.string.details_paid)
        )

        if (app.containsAds)
            extras.add(ContextCompat.getString(context, R.string.details_contains_ads))

        if (app.dependencies.dependentPackages.isNotEmpty())
            extras.add(ContextCompat.getString(context, R.string.details_gsf_dependent))

        binding.txtLine3.text = extras.joinToString(separator = "  •  ")
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
