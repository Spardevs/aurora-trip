package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.dao.PosDao
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.entity.PosEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosRepository @Inject constructor(
    private val posDao: PosDao
) {

    suspend fun insertPosList(posList: List<PosEntity>) {
        return posDao.insertPosList(posList)
    }

    suspend fun getAll() = posDao.getAll()

    suspend fun updatePos(posEntity: PosEntity) = posDao.updatePosItem(posEntity)

    suspend fun clearAll() = posDao.clearPosList()

    suspend fun getSelectedPos() = posDao.getSelectedPos()

    suspend fun selectPos(eventId: String) {

        val oldPosItem = posDao.getSelectedPos()

        if(oldPosItem != null) {
            oldPosItem.isClosed = false
            posDao.updatePosItem(oldPosItem)
        }

        val posToUpdate = posDao.getById(eventId)

        if (posToUpdate != null) {
            posToUpdate.isSelected = true
            posDao.updatePosItem(posToUpdate)
        }
    }

    suspend fun unSelectPos(eventId: String) {

        val oldPosItem = posDao.getSelectedPos()

        if(oldPosItem != null) {
            oldPosItem.isClosed = false
            posDao.updatePosItem(oldPosItem)
        }

        val posToUpdate = posDao.getById(eventId)

        if (posToUpdate != null) {
            posToUpdate.isSelected = false
            posDao.updatePosItem(posToUpdate)
        }
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: EventRepository? = null

        fun getInstance(eventDao: EventDao) =
            instance ?: synchronized(this) {
                instance ?: EventRepository(eventDao).also { instance = it }
            }
    }
}
