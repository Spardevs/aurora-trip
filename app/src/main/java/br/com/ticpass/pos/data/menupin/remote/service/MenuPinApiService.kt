package br.com.ticpass.pos.data.menupin.remote.service

import br.com.ticpass.pos.data.menupin.remote.dto.MenuPinDto
import retrofit2.http.GET
import retrofit2.http.Path

interface MenuPinApiService {
    
    /**
     * Get menu pin summary (whitelist of allowed pins for a menu)
     * @param menuId The menu ID to get pins for
     * @return List of menu pins with user information
     */
    @GET("menu-pin-summary/{menuId}")
    suspend fun getMenuPinSummary(
        @Path("menuId") menuId: String
    ): List<MenuPinDto>
}
