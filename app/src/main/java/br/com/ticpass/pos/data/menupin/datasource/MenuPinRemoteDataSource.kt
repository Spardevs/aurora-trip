package br.com.ticpass.pos.data.menupin.datasource

import br.com.ticpass.pos.data.menupin.remote.dto.MenuPinDto
import br.com.ticpass.pos.data.menupin.remote.service.MenuPinApiService
import timber.log.Timber
import javax.inject.Inject

class MenuPinRemoteDataSource @Inject constructor(
    private val apiService: MenuPinApiService
) {
    
    suspend fun getMenuPinSummary(menuId: String): List<MenuPinDto> {
        Timber.tag("MenuPin").i("Fetching menu pins for menuId: $menuId")
        val response = apiService.getMenuPinSummary(menuId)
        Timber.tag("MenuPin").i("Received ${response.size} menu pins")
        return response
    }
}
