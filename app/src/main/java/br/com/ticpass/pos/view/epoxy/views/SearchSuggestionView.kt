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
import androidx.core.content.ContextCompat
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.gplayapi.SearchSuggestEntry
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewSearchSuggestionBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class SearchSuggestionView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewSearchSuggestionBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun entry(searchSuggestEntry: SearchSuggestEntry) {
        if (searchSuggestEntry.hasImageContainer()) {
            binding.img.load(searchSuggestEntry.imageContainer.imageUrl) {
                transformations(RoundedCornersTransformation(8F))
            }
        } else {
            binding.img.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_search_suggestion
                )
            )
        }

        binding.txtTitle.text = searchSuggestEntry.title
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun action(onClickListener: OnClickListener?) {
        binding.action.setOnClickListener(onClickListener)
    }
}
