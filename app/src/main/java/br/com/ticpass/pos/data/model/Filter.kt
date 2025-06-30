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

import kotlinx.serialization.Serializable

@Serializable
data class Filter(
    val appsWithAds: Boolean = true,
    val appsWithIAP: Boolean = true,
    val paidApps: Boolean = true,
    val gsfDependentApps: Boolean = true,
    val rating: Float = 0.0f,
    val downloads: Int = 0
)
