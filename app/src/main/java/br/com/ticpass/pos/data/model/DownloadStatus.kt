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

package br.com.ticpass.pos.data.model

import androidx.annotation.StringRes
import br.com.ticpass.pos.R

enum class DownloadStatus(@StringRes val localized: Int) {
    DOWNLOADING(R.string.status_downloading),
    FAILED(R.string.status_failed),
    CANCELLED(R.string.status_cancelled),
    COMPLETED(R.string.status_completed),
    QUEUED(R.string.status_queued),
    UNAVAILABLE(R.string.status_unavailable),
    VERIFYING(R.string.status_verifying);

    companion object {
        val finished = listOf(FAILED, CANCELLED, COMPLETED)
        val running = listOf(QUEUED, DOWNLOADING)
    }
}
