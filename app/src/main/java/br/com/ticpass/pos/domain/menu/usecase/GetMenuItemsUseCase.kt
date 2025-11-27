package br.com.ticpass.pos.domain.menu.usecase

import br.com.ticpass.pos.domain.menu.repository.MenuRepository
import br.com.ticpass.pos.domain.menu.model.MenuDb
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMenuItemsUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    operator fun invoke(take: Int, page: Int): Flow<List<MenuDb>> {
        return menuRepository.getMenuItems(take, page)
    }
}