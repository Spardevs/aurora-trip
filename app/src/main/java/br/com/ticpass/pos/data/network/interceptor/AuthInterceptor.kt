package br.com.ticpass.pos.data.network.interceptor

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AuthInterceptor(private val context: Context) : Interceptor {
    private val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    override fun intercept(chain: Interceptor.Chain): Response {
        val expirationStr = prefs.getString("token_expiration", null)
        expirationStr?.let {
            val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT' yyyy", Locale.ENGLISH)
            val expirationDate = sdf.parse(it)
            if (expirationDate != null && expirationDate.before(Date())) {
                throw IOException("Token expirado em $it")
            }
        }

        val token = prefs.getString("auth_token", "") ?: ""
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}
