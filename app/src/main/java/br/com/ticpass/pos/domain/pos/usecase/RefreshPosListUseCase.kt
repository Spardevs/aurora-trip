package br.com.ticpass.pos.domain.pos.usecase

import br.com.ticpass.pos.domain.pos.model.Pos
import br.com.ticpass.pos.domain.pos.repository.PosRepository

class RefreshPosListUseCase(private val repository: PosRepository) {
    suspend operator fun invoke(
        take: Int,
        page: Int,
        menu: String,
        available: String,
        authorization: String,
        cookie: String
    ): Result<List<Pos>> = repository.refreshPosList(
        take, page, menu, available, authorization, cookie
    )
}