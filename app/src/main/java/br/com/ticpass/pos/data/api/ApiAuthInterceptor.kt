package br.com.ticpass.pos.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class ApiAuthInterceptor(
    private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        val sp = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val accessToken = sp.getString("auth_token", null)
        val refreshToken = sp.getString("refresh_token", null)

        // Header Authorization: usa refresh_token
        if (!refreshToken.isNullOrBlank()) {
            builder.header("Authorization", refreshToken)
        }

        // Cookie: access=<auth_token>
        if (!accessToken.isNullOrBlank() && original.header("Cookie").isNullOrBlank()) {
            builder.header("Cookie", "access=$accessToken")
        }

        return chain.proceed(builder.build())
    }
}