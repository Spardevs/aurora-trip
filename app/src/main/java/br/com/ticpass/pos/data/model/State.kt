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


sealed class ViewState {
    inline fun <reified T> ViewState.getDataAs(): T {
        return (this as? Success<*>)?.data as T
    }

    data object Loading : ViewState()
    data object Empty : ViewState()
    data class Error(val error: String?) : ViewState()
    data class Status(val status: String?) : ViewState()
    data class Success<T>(val data: T) : ViewState()
}

sealed class AuthState {
    data object Init: AuthState()
    data object Available : AuthState()
    data object Unavailable : AuthState()
    data object SignedIn : AuthState()
    data object SignedOut : AuthState()
    data object Valid : AuthState()
    data object Fetching : AuthState()
    data object Verifying : AuthState()
    data class PendingAccountManager(val email: String, val token: String) : AuthState()
    data class Failed(val status: String) : AuthState()
}
