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

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale

fun Long.toDate(): String {
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = this
    return DateFormat.format("dd/MM/yy", calendar).toString()
}
