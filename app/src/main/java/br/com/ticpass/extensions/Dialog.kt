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
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Context.showDialog(@StringRes titleId: Int, @StringRes messageId: Int) {
    showDialog(getString(titleId), getString(messageId), null, null)
}

fun Context.showDialog(title: String?, message: String?) {
    showDialog(title, message, null, null)
}

fun Context.showDialog(
    title: String?,
    message: String?,
    positiveListener: DialogInterface.OnClickListener?,
    negativeListener: DialogInterface.OnClickListener?
) {
    runOnUiThread {
        val builder = MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setMessage(message)

            if (positiveListener != null) {
                setPositiveButton(android.R.string.ok, positiveListener)
            } else {
                setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            }

            negativeListener?.let {
                setNegativeButton(android.R.string.cancel, negativeListener)
            }

        }.create()

        builder.show()
    }
}

fun Fragment.showDialog(@StringRes titleId: Int, @StringRes messageId: Int) {
    requireContext().showDialog(titleId, messageId)
}

fun Fragment.showDialog(title: String, message: String) {
    requireContext().showDialog(title, message)
}
