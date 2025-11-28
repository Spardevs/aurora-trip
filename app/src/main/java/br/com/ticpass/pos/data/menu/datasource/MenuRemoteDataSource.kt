package br.com.ticpass.pos.data.menu.datasource

import br.com.ticpass.pos.data.menu.remote.service.MenuApiService
import br.com.ticpass.pos.data.menu.remote.dto.MenuResponse
import br.com.ticpass.pos.data.menu.remote.service.MenuLogoService
import okhttp3.ResponseBody
import javax.inject.Inject

class MenuRemoteDataSource @Inject constructor(
    private val apiService: MenuApiService,
    private val logoService: MenuLogoService
) {
    suspend fun getMenu(take: Int, page: Int): MenuResponse =
        apiService.getMenu(take, page)

    suspend fun downloadLogo(logoId: String): ResponseBody =
        logoService.downloadLogo(logoId)
}