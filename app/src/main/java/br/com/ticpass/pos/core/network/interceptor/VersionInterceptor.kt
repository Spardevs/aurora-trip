package br.com.ticpass.pos.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class VersionInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithVersion = originalRequest.newBuilder()
            .header("version", "2.0.0")
            .build()

        return chain.proceed(requestWithVersion)
    }
}