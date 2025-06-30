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

package br.com.ticpass.pos.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.Preferences

/**
 * App theme for Aurora Store based on [MaterialTheme]
 */
@Composable
fun AuroraTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeStyle = Preferences.getInteger(context, Preferences.PREFERENCE_THEME_STYLE)
    val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val lightScheme = if (isDynamicColorSupported) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme(primary = colorResource(id = R.color.colorAccent))
    }

    val darkScheme = if (isDynamicColorSupported) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme(primary = colorResource(id = R.color.colorAccent))
    }

    val colorScheme = when (themeStyle) {
        1 -> lightScheme
        2 -> darkScheme
        else -> if (isSystemInDarkTheme()) darkScheme else lightScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
