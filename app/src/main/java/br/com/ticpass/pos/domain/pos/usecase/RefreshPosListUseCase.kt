package br.com.ticpass.pos.domain.pos.usecase

import br.com.ticpass.pos.domain.pos.model.Pos
import br.com.ticpass.pos.domain.pos.repository.PosRepository
import javax.inject.Inject

class RefreshPosListUseCase @Inject constructor(
    private val posRepository: PosRepository
) {
    suspend operator fun invoke(
        take: Int,
        page: Int,
        menu: String,
        available: String
    ): Result<List<Pos>> =
        posRepository.refreshPosList(take, page, menu, available)
}