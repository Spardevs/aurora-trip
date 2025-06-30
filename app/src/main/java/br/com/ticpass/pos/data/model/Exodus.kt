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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class ExodusReport(
    val creator: String = String(),
    val name: String = String(),
    val reports: List<Report> = listOf()
)

@Serializable
@Parcelize
data class Report(
    val id: Int = 0,
    val downloads: String = String(),
    val version: String = String(),
    val creationDate: String = String(),
    val updatedAt: String = String(),
    val versionCode: String = String(),
    val trackers: List<Int> = listOf()
) : Parcelable

@Serializable
data class ExodusTracker(
    val id: Int = 0,
    val name: String = String(),
    val url: String = String(),
    val signature: String = String(),
    val date: String = String(),
    val description: String = String(),
    val networkSignature: String = String(),
    val documentation: List<String> = emptyList(),
    val categories: List<String> = emptyList()
) {

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ExodusTracker -> other.id == id
            else -> false
        }
    }
}
