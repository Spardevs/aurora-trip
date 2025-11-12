package br.com.ticpass.pos.data.api

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class Api2Repository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val service: Api2Service
) {

    suspend fun signInShortLived(
        shortLivedToken: String,
        proxyCredentials: String
    ): ShortLivedSignInResponse {
        return try {
            // ✅ Formato exato conforme cURL
            val cookie = "shortLived=$shortLivedToken;"
            val authorization = proxyCredentials

            // ✅ Corpo JSON vazio: {}
            val emptyJson = "{}".toRequestBody("application/json".toMediaType())

            Log.d("Api2Repository", "Cookie: $cookie")
            Log.d("Api2Repository", "Authorization: $authorization")

            val response = service.signInShortLived(
                cookie = cookie,
                authorization = authorization,
                body = emptyJson
            )

            Log.d("Api2Repository", "SignIn response: status=${response.status}, message=${response.message}")
            response
        } catch (e: Exception) {
            Log.e("Api2Repository", "Erro ao fazer signIn", e)
            ShortLivedSignInResponse(
                status = 500,
                message = "Erro ao realizar requisição: ${e.message}",
                result = null,
                error = e.message,
                name = "exception"
            )
        }
    }
}