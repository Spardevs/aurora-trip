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
import br.com.ticpass.pos.data.model.DownloadStatus
import br.com.ticpass.pos.data.room.download.Download
import br.com.ticpass.pos.databinding.ViewDownloadBinding
import br.com.ticpass.pos.util.CommonUtil.getDownloadSpeedString
import br.com.ticpass.pos.util.CommonUtil.getETAString

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class DownloadView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewDownloadBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun download(download: Download) {
        binding.imgDownload.load(download.iconURL) {
            placeholder(R.drawable.bg_placeholder)
            transformations(RoundedCornersTransformation(32F))
        }
        binding.txtTitle.text = download.displayName
        binding.txtStatus.text = context.getString(download.downloadStatus.localized)

        binding.progressDownload.apply {
            progress = download.progress
            isIndeterminate = download.progress <= 0 && !download.isFinished
        }
        binding.txtProgress.text = ("${download.progress}%")

        binding.txtEta.text = getETAString(context, download.timeRemaining)
        binding.txtSpeed.text = getDownloadSpeedString(
            context,
            download.speed
        )

        when (download.downloadStatus) {
            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                binding.txtSpeed.visibility = VISIBLE
                binding.txtEta.visibility = VISIBLE
            }

            else -> {
                binding.txtSpeed.visibility = INVISIBLE
                binding.txtEta.visibility = INVISIBLE
            }
        }
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
