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
import android.view.View
import android.widget.RelativeLayout
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewActionHeaderBinding

class ActionHeaderLayout : RelativeLayout {

    private lateinit var binding: ViewActionHeaderBinding

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
        val view = inflate(context, R.layout.view_action_header, this)
        binding = ViewActionHeaderBinding.bind(view)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionHeaderLayout)
        val textTitle = typedArray.getString(R.styleable.ActionHeaderLayout_headerTitle)
        val textSubtitle = typedArray.getString(R.styleable.ActionHeaderLayout_headerSubtitle)

        typedArray.recycle()

        textTitle?.let {
            binding.txtTitle.text = it
        }

        textSubtitle?.let {
            binding.txtSubtitle.visibility = View.VISIBLE
            binding.txtSubtitle.text = it
        }
    }

    fun setTitle(header: String?) {
        binding.txtTitle.text = header
    }

    fun setSubTitle(subHeader: String?) {
        binding.txtSubtitle.visibility = View.VISIBLE
        binding.txtSubtitle.text = subHeader
    }

    fun addClickListener(onclickListener: OnClickListener?) {
        binding.imgAction.visibility = View.VISIBLE
        binding.imgAction.setOnClickListener(onclickListener)
    }
}