package br.com.ticpass.pos.domain.pos.repository

import br.com.ticpass.pos.domain.pos.model.Pos
import kotlinx.coroutines.flow.Flow

interface PosRepository {
    fun getPosByMenu(menuId: String): Flow<List<Pos>>
    suspend fun refreshPosList(
        take: Int,
        page: Int,
        menu: String,
        available: String,
        authorization: String,
        cookie: String
    ): Result<List<Pos>>
}