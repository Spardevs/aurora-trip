package br.com.ticpass.pos.domain.menupin.usecase

import br.com.ticpass.pos.domain.menupin.model.MenuPin
import br.com.ticpass.pos.domain.menupin.repository.MenuPinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting menu pins (reactive Flow)
 */
class GetMenuPinsUseCase @Inject constructor(
    private val menuPinRepository: MenuPinRepository
) {
    
    operator fun invoke(menuId: String): Flow<List<MenuPin>> {
        return menuPinRepository.getPinsByMenuId(menuId)
    }
}
