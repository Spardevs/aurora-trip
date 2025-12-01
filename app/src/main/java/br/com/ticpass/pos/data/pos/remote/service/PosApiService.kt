package br.com.ticpass.pos.data.pos.remote.service

import br.com.ticpass.pos.data.pos.remote.dto.PosResponseDto
import br.com.ticpass.pos.data.pos.remote.dto.SessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface PosApiService {
    @GET("menu-pos")
    suspend fun getPosList(
        @Query("take") take: Int,
        @Query("page") page: Int,
        @Query("menu") menu: String,
        @Query("available") available: String,
    ): Response<PosResponseDto>

    @POST("menu-pos-sessions/open")
    suspend fun openPosSession(
        @Body openSessionRequest: OpenSessionRequest
    ): Response<SessionDto>

    @PUT("menu-pos-sessions/close")
    suspend fun closePosSession(
        @Body closeSessionRequest: CloseSessionRequest
    ): Response<SessionDto>
}

data class OpenSessionRequest(
    val pos: String,
    val device: String,
    val cashier: String
)

data class CloseSessionRequest(
    val id: String
)