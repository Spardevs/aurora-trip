package br.com.ticpass.pos.data.menu.datasource

import br.com.ticpass.pos.data.menu.remote.service.MenuApiService
import br.com.ticpass.pos.data.menu.remote.dto.MenuResponse
import br.com.ticpass.pos.data.menu.remote.service.MenuLogoService
import okhttp3.ResponseBody
import timber.log.Timber
import javax.inject.Inject

class MenuRemoteDataSource @Inject constructor(
    private val apiService: MenuApiService,
    private val logoService: MenuLogoService
) {
    suspend fun getMenu(take: Int, page: Int): MenuResponse {
        Timber.tag("MenuRemoteDataSource").d("getMenu called take=$take page=$page")
        return try {
            val resp = apiService.getMenu(take, page)
            Timber.tag("MenuRemoteDataSource").d("getMenu response ok edges=${resp.edges.size}")
            resp
        } catch (e: Exception) {
            Timber.tag("MenuRemoteDataSource").e(e, "getMenu failed")
            throw e
        }
    }

    suspend fun downloadLogo(logoId: String): ResponseBody =
        logoService.downloadLogo(logoId)
}