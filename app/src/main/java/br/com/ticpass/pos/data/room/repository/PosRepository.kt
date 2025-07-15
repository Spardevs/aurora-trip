package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.data.room.dao.PosDao
import br.com.ticpass.pos.data.room.entity.PosEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosRepository @Inject constructor(
    private val posDao: PosDao
) {

    suspend fun insertPosList(posList: List<PosEntity>) {
        posDao.insertPosList(posList)
    }

    suspend fun getAll(): List<PosEntity> =
        posDao.getAll()

    suspend fun upsertPos(pos: PosEntity) =
        posDao.upsertPos(pos)

    suspend fun updatePos(posEntity: PosEntity) =
        posDao.updatePosItem(posEntity)

    suspend fun clearAll() =
        posDao.clearPosList()

    /**
     * Como getSelectedPos() retorna PosEntity não-nulo, não precisamos checar null.
     */
    suspend fun getSelectedPos(): PosEntity =
        posDao.getSelectedPos()

    /**
     * Marca o atual como fechado e seleciona o novo.
     */
    suspend fun selectPos(eventId: String) {
        // fecha o antigo
        val oldPos = posDao.getSelectedPos()
        oldPos.isClosed = true
        posDao.updatePosItem(oldPos)

        // seleciona o novo (já não-null)
        val newPos = posDao.getById(eventId)
        newPos?.isSelected = true
        posDao.updatePosItem((newPos ?: intArrayOf()) as PosEntity)
    }

    /**
     * Desselciona o indicado (sem checagem de null, pois getById não é nulo).
     */
    suspend fun unSelectPos(eventId: String) {
        val oldPos = posDao.getSelectedPos()
        oldPos.isClosed = true
        posDao.updatePosItem(oldPos)

        val pos = posDao.getById(eventId)
        pos?.isSelected = false
        posDao.updatePosItem((pos ?: intArrayOf()) as PosEntity)
    }

    companion object {
        @Volatile
        private var instance: PosRepository? = null

        fun getInstance(posDao: PosDao): PosRepository =
            instance ?: synchronized(this) {
                instance ?: PosRepository(posDao).also { instance = it }
            }
    }
}
