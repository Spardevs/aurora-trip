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
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Context.toast(resId: Int) = runOnUiThread {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).apply { show() }
}

fun Context.toast(text: CharSequence) = runOnUiThread {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).apply { show() }
}

fun Fragment.toast(resId: Int) = requireContext().toast(resId)

fun Fragment.toast(text: CharSequence) = requireContext().toast(text)
