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

package br.com.ticpass.pos.data.event

abstract class Event

sealed class BusEvent : Event() {
    lateinit var extra: String
    lateinit var error: String

    data class Blacklisted(val packageName: String) : BusEvent()
    data class ManualDownload(val packageName: String, val versionCode: Long) : BusEvent()
}

sealed class AuthEvent : Event() {
    data class GoogleLogin(val success: Boolean, val email: String, val token: String) : AuthEvent()
}

sealed class InstallerEvent : Event() {
    lateinit var extra: String
    lateinit var error: String

    var progress: Int = -1

    data class Installed(val packageName: String) : InstallerEvent()
    data class Uninstalled(val packageName: String) : InstallerEvent()
    data class Installing(val packageName: String) : InstallerEvent()
    data class Cancelled(val packageName: String) : InstallerEvent()
    data class Failed(val packageName: String) : InstallerEvent()
}
