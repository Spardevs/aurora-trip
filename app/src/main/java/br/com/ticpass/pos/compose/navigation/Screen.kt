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

package br.com.ticpass.pos.compose.navigation

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import br.com.ticpass.pos.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Destination (Screen) for navigation in compose
 * @param label Label of the screen
 * @param icon Optional icon for the screen; Must not be null if screen is a top-level destination
 */
@Parcelize
@Serializable
sealed class Screen(@StringRes val label: Int, @DrawableRes val icon: Int? = null): Parcelable {

    companion object {
        const val PARCEL_KEY = "SCREEN"
    }

    @Serializable
    data object Blacklist : Screen(label = R.string.title_blacklist_manager)
}
