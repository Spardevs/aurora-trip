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

package br.com.ticpass.pos.util

import br.com.ticpass.pos.data.network.HttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import javax.inject.Inject

class AC2DMTask @Inject constructor(private val httpClient: HttpClient) {

    @Throws(Exception::class)
    fun getAC2DMResponse(email: String?, oAuthToken: String?): Map<String, String> {
        if (email == null || oAuthToken == null)
            return mapOf()

        val params: MutableMap<String, Any> = hashMapOf()
        params["lang"] = Locale.getDefault().toString().replace("_", "-")
        params["google_play_services_version"] = PLAY_SERVICES_VERSION_CODE
        params["sdk_version"] = BUILD_VERSION_SDK
        params["device_country"] = Locale.getDefault().country.lowercase(Locale.US)
        params["Email"] = email
        params["service"] = "ac2dm"
        params["get_accountid"] = 1
        params["ACCESS_TOKEN"] = 1
        params["callerPkg"] = "com.google.android.gms"
        params["add_account"] = 1
        params["Token"] = oAuthToken
        params["callerSig"] = "38918a453d07199354f8b19af05ec6562ced5788"

        val body = params.map { "${it.key}=${it.value}" }.joinToString(separator = "&")

        val header = mapOf(
            "app" to "com.google.android.gms",
            "User-Agent" to "",
            "Content-Type" to "application/x-www-form-urlencoded"
        )

        val response = httpClient.post(TOKEN_AUTH_URL, header, body.toRequestBody())

        return if (response.isSuccessful) {
            br.com.ticpass.pos.util.AC2DMUtil.parseResponse(String(response.responseBytes))
        } else {
            mapOf()
        }
    }

    companion object {
        private const val TOKEN_AUTH_URL = "https://android.clients.google.com/auth"
        private const val BUILD_VERSION_SDK = 28
        private const val PLAY_SERVICES_VERSION_CODE = 19629032
    }
}
