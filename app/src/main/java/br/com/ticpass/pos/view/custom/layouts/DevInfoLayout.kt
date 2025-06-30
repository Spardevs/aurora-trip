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

package br.com.ticpass.pos.view.custom.layouts

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewDevInfoBinding

class DevInfoLayout : RelativeLayout {

    private lateinit var binding: ViewDevInfoBinding

    val icon: AppCompatImageView get() = binding.img

    var title: String
        get() = binding.txtTitle.text.toString()
        set(value) = setTxtTitle(value)

    var subTitle: String?
        get() = binding.txtSubtitle.text.toString()
        set(value) = setTxtSubtitle(value)

    @get:ColorInt
    var titleColor: Int
        get() = binding.txtTitle.currentTextColor
        set(value) = binding.txtTitle.setTextColor(value)

    @get:ColorInt
    var subTitleColor: Int
        get() = binding.txtSubtitle.currentTextColor
        set(value) = binding.txtSubtitle.setTextColor(value)

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val view = inflate(context, R.layout.view_dev_info, this)
        binding = ViewDevInfoBinding.bind(view)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DevInfoLayout)
        val icon = typedArray.getResourceId(
            R.styleable.DevInfoLayout_imgIcon,
            R.drawable.ic_map_marker
        )

        val textPrimary = typedArray.getString(R.styleable.DevInfoLayout_txtTitle)
        val textSecondary = typedArray.getString(R.styleable.DevInfoLayout_txtSubtitle)

        binding.img.setImageResource(icon)
        binding.txtTitle.text = textPrimary
        binding.txtSubtitle.text = textSecondary
        typedArray.recycle()
    }

    private fun setTxtTitle(text: String?) {
        binding.txtTitle.text = text
        binding.txtTitle.isVisible = text != null
        invalidate()
    }

    private fun setTxtSubtitle(text: String?) {
        binding.txtSubtitle.text = text
        binding.txtSubtitle.isVisible = text != null
        invalidate()
    }
}
