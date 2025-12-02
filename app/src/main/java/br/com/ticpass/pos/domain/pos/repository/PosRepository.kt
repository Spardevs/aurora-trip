package br.com.ticpass.pos.domain.pos.repository

import br.com.ticpass.pos.domain.pos.model.Pos
import br.com.ticpass.pos.domain.pos.model.Session
import kotlinx.coroutines.flow.Flow

interface PosRepository {
    fun getPosByMenu(menuId: String): Flow<List<Pos>>
    suspend fun refreshPosList(
        take: Int,
        page: Int,
        menu: String,
        available: String
    ): Result<List<Pos>>

    suspend fun selectPos(posId: String): Result<Unit>
    suspend fun openPosSession(posId: String, deviceId: String, cashierId: String): Result<Session>
}