package br.com.ticpass.pos.domain.menupin.usecase

import br.com.ticpass.pos.domain.menupin.repository.MenuPinRepository
import javax.inject.Inject

/**
 * Use case for refreshing menu pins from the remote API
 */
class RefreshMenuPinsUseCase @Inject constructor(
    private val menuPinRepository: MenuPinRepository
) {
    
    suspend operator fun invoke(menuId: String) {
        menuPinRepository.refreshPins(menuId)
    }
}
