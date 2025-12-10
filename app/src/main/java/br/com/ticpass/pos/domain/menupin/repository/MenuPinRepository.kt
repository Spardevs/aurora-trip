package br.com.ticpass.pos.domain.menupin.repository

import br.com.ticpass.pos.domain.menupin.model.MenuPin
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for menu pin operations
 */
interface MenuPinRepository {
    
    /**
     * Get all pins for a menu as a Flow (reactive)
     */
    fun getPinsByMenuId(menuId: String): Flow<List<MenuPin>>
    
    /**
     * Get all pins for a menu (one-shot)
     */
    suspend fun getPinsByMenuIdOnce(menuId: String): List<MenuPin>
    
    /**
     * Validate if a pin code is allowed for a menu
     * @return The MenuPin if valid, null otherwise
     */
    suspend fun validatePin(menuId: String, code: String): MenuPin?
    
    /**
     * Refresh pins from remote API and store locally
     */
    suspend fun refreshPins(menuId: String)
    
    /**
     * Get count of pins for a menu
     */
    suspend fun countPins(menuId: String): Int
}
