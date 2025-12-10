package br.com.ticpass.pos.domain.menupin.usecase

import br.com.ticpass.pos.domain.menupin.model.MenuPin
import br.com.ticpass.pos.domain.menupin.repository.MenuPinRepository
import javax.inject.Inject

/**
 * Use case for validating a pin code against the menu whitelist
 */
class ValidateMenuPinUseCase @Inject constructor(
    private val menuPinRepository: MenuPinRepository
) {
    
    /**
     * Validate if a pin code is allowed for a menu
     * @param menuId The menu ID
     * @param code The pin code to validate
     * @return The MenuPin if valid, null otherwise
     */
    suspend operator fun invoke(menuId: String, code: String): MenuPin? {
        return menuPinRepository.validatePin(menuId, code)
    }
}
