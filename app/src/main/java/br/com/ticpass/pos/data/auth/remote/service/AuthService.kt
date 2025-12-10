package br.com.ticpass.pos.data.auth.remote.service

import br.com.ticpass.pos.data.auth.remote.dto.LoginResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthService {
    @POST("auth/signin/pos")
    suspend fun signIn(
        @Body requestBody: RequestBody
    ): Response<LoginResponse>

    @POST("auth/signin/pos/short-lived")
    suspend fun signInShortLived(
        @Body requestBody: RequestBody,
        @Header("Cookie") cookieHeader: String
    ): Response<LoginResponse>
}