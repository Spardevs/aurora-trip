package br.com.ticpass.pos.data.pos.remote.service

import br.com.ticpass.pos.data.pos.remote.dto.PosResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PosApiService {
    @GET("menu-pos")
    suspend fun getPosList(
        @Query("take") take: Int,
        @Query("page") page: Int,
        @Query("menu") menu: String,
        @Query("available") available: String,
        @Header("Authorization") authorization: String,
        @Header("Cookie") cookie: String
    ): Response<PosResponseDto>
}