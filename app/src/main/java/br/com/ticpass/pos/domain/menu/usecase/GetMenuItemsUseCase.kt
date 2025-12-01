package br.com.ticpass.pos.domain.menu.usecase

import br.com.ticpass.pos.domain.menu.repository.MenuRepository
import br.com.ticpass.pos.domain.menu.model.Menu
import br.com.ticpass.pos.domain.menu.model.MenuDb
import br.com.ticpass.pos.domain.menu.model.EventDate
import br.com.ticpass.pos.domain.menu.model.PassInfo
import br.com.ticpass.pos.domain.menu.model.PaymentInfo
import br.com.ticpass.pos.domain.menu.model.AccountableInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMenuItemsUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    operator fun invoke(take: Int, page: Int): Flow<List<Menu>> {
        return menuRepository.getMenuItems(take, page)
            .map { dbList -> dbList.map { it.toDomain() } }
    }
}

/**
 * Map DB entity -> Domain model.
 *
 * NOTE: MenuDb doesn't carry all fields required by Menu, so we provide sensible defaults.
 * Adjust these defaults to reflect real data if you have it available (or expand MenuDb).
 */
private fun MenuDb.toDomain(): Menu {
    return Menu(
        id = this.id,
        label = this.name,                  // map DB name -> domain label
        status = "",                         // default if DB doesn't have status
        mode = this.mode,
        logo = this.logo,                    // logo id/path
        goal = 0L,                           // default goal (adapt if needed)
        date = br.com.ticpass.pos.data.menu.remote.dto.EventDate(
            start = this.dateStart,
            end = this.dateEnd
        ),
        pass = br.com.ticpass.pos.data.menu.remote.dto.PassInfo(
            vouchering = false,
            pricing = false,
            mode = this.mode ?: "",
            description = this.details
        ),
        payment = br.com.ticpass.pos.data.menu.remote.dto.PaymentInfo(
            methods = emptyList(),
            multi = false,
            acquirer = false
        ),
        team = emptyList(),
        accountable = br.com.ticpass.pos.data.menu.remote.dto.AccountableInfo(id = "", name = ""),
        createdBy = "",
        createdAt = "",
        updatedAt = ""
    )
}