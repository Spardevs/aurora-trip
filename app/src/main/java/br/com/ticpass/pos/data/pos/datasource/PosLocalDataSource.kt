package br.com.ticpass.pos.data.pos.datasource

import br.com.ticpass.pos.data.pos.local.dao.PosDao
import br.com.ticpass.pos.data.pos.local.entity.PosEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosLocalDataSource @Inject constructor(private val dao: PosDao) {

    fun getPosByMenu(menuId: String): Flow<List<PosEntity>> = dao.getPosByMenu(menuId)

    suspend fun savePosList(list: List<PosEntity>) = dao.insertAll(list)

    suspend fun clearAll() = dao.deleteAll()
}