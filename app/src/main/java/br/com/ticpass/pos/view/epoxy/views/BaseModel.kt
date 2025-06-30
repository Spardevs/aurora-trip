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

import android.view.View
import android.view.animation.AnimationUtils
import com.airbnb.epoxy.EpoxyModel
import br.com.ticpass.pos.view.epoxy.views.app.AppListView

abstract class BaseModel<T : View> : EpoxyModel<T>() {

    override fun bind(view: T) {
        super.bind(view)
        when (view) {
            is AppListView -> {
                view.startAnimation(
                    AnimationUtils.loadAnimation(
                        view.context,
                        android.R.anim.fade_in
                    )
                )
            }
        }
    }

    override fun unbind(view: T) {
        when (view) {
            is AppListView -> {
                view.clearAnimation()
            }
        }
    }
}
