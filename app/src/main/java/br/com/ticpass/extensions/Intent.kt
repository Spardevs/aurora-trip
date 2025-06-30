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

package br.com.ticpass.extensions

import android.content.Context
import android.content.Intent
import android.net.UrlQuerySanitizer
import android.os.Bundle

inline fun <reified T : Context> Context.newIntent(): Intent =
    Intent(this, T::class.java)

inline fun <reified T : Context> Context.newIntent(flags: Int): Intent {
    val intent = newIntent<T>()
    intent.flags = flags
    return intent
}

inline fun <reified T : Context> Context.newIntent(extras: Bundle): Intent =
    newIntent<T>(0, extras)

inline fun <reified T : Context> Context.newIntent(flags: Int, extras: Bundle): Intent {
    val intent = newIntent<T>(flags)
    intent.putExtras(extras)
    return intent
}

fun Intent.getPackageName(fallbackBundle: Bundle? = null): String? {
    return when (action) {
        Intent.ACTION_VIEW -> {
            data?.getQueryParameter("id")
        }
        Intent.ACTION_SEND -> {
            val clipData = getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            UrlQuerySanitizer(clipData).getValue("id")
        }
        Intent.ACTION_SHOW_APP_INFO -> {
            extras?.getString(Intent.EXTRA_PACKAGE_NAME)
        }
        else -> {
            extras?.getString("packageName") ?: fallbackBundle?.getString("packageName")
        }
    }
}

