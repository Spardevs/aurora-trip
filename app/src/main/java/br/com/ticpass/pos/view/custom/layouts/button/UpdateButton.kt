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
import android.widget.RelativeLayout
import br.com.ticpass.extensions.runOnUiThread
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.DownloadStatus
import br.com.ticpass.pos.databinding.ViewUpdateButtonBinding

class UpdateButton : RelativeLayout {

    private lateinit var binding: ViewUpdateButtonBinding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        val view = inflate(context, R.layout.view_update_button, this)
        binding = ViewUpdateButtonBinding.bind(view)
    }

    fun updateState(downloadStatus: DownloadStatus) {
        val displayChild = when (downloadStatus) {
            DownloadStatus.QUEUED,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.VERIFYING -> 1

            else -> 0
        }

        if (binding.viewFlipper.displayedChild != displayChild) {
            runOnUiThread {
                binding.viewFlipper.displayedChild = displayChild
            }
        }

        // Not allowed to cancel installation at this point
        binding.btnNegative.isEnabled = downloadStatus != DownloadStatus.VERIFYING
    }

    fun addPositiveOnClickListener(onClickListener: OnClickListener?) {
        binding.btnPositive.setOnClickListener(onClickListener)
    }

    fun addNegativeOnClickListener(onClickListener: OnClickListener?) {
        binding.btnNegative.setOnClickListener(onClickListener)
    }
}
