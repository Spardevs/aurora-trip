package br.com.ticpass.pos.data.api

import okhttp3.Interceptor
import okhttp3.Response

class VersionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithVersion = originalRequest.newBuilder()
            .header("version", "2.0.0")
            .build()

        return chain.proceed(requestWithVersion)
    }
}