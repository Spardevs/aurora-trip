package br.com.ticpass.pos.data.menu.remote.service

import br.com.ticpass.pos.data.menu.remote.dto.MenuResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MenuApiService {
    @GET("menu")
    suspend fun getMenu(
        @Query("take") take: Int,
        @Query("page") page: Int
    ): MenuResponse
}

interface MenuLogoService {
    @GET("menu/logo/{logoId}/download")
    suspend fun downloadLogo(
        @Path("logoId") logoId: String
    ): ResponseBody
}