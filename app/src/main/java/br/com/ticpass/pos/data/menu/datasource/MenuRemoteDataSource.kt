package br.com.ticpass.pos.data.menu.datasource

import br.com.ticpass.pos.data.menu.remote.service.MenuApiService
import br.com.ticpass.pos.data.menu.remote.dto.MenuResponse
import br.com.ticpass.pos.data.menu.remote.service.MenuLogoService
import okhttp3.ResponseBody
import javax.inject.Inject

class MenuRemoteDataSource @Inject constructor(
    private val apiService: MenuApiService?,
    private val logoService: MenuLogoService?
) {
    suspend fun getMenu(take: Int, page: Int): MenuResponse {
        return apiService?.getMenu(take, page) ?: throw Exception("MenuApiService is null")
    }

    suspend fun downloadLogo(logoId: String): ResponseBody {
        return logoService?.downloadLogo(logoId) ?: throw Exception("MenuLogoService is null")
    }
}