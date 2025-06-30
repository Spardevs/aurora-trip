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

package br.com.ticpass.pos.view.custom.layouts.button

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ViewStateButtonBinding

class StateButton : RelativeLayout {

    private lateinit var binding: ViewStateButtonBinding

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
        val view = inflate(context, R.layout.view_state_button, this)
        binding = ViewStateButtonBinding.bind(view)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.StateButton)
        val btnTxt = typedArray.getString(R.styleable.StateButton_btnStateText)
        val btnIcon = typedArray.getResourceId(
            R.styleable.StateButton_btnStateIcon,
            R.drawable.ic_arrow_right
        )

        binding.btn.text = btnTxt
        binding.btn.icon = ContextCompat.getDrawable(context, btnIcon)
        typedArray.recycle()
    }

    fun updateProgress(isVisible: Boolean) {
        if (isVisible)
            binding.progress.visibility = View.VISIBLE
        else

            binding.progress.visibility = View.INVISIBLE
    }

    fun addOnClickListener(onClickListener: OnClickListener) {
        binding.btn.setOnClickListener(onClickListener)
    }
}
