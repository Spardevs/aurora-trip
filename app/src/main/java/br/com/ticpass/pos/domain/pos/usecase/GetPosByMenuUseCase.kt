package br.com.ticpass.pos.domain.pos.usecase

import br.com.ticpass.pos.domain.pos.model.Pos
import br.com.ticpass.pos.domain.pos.repository.PosRepository
import kotlinx.coroutines.flow.Flow

class GetPosByMenuUseCase(private val repository: PosRepository) {
    operator fun invoke(menuId: String): Flow<List<Pos>> = repository.getPosByMenu(menuId)
}