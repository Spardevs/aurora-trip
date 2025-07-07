package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.dao.PassDao
import br.com.ticpass.pos.data.room.entity.PassEntity
import br.com.ticpass.pos.data.room.entity.PassPopulated
import br.com.ticpass.pos.dataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassRepository @Inject constructor(
    private val passDao: PassDao
) {
    suspend fun deleteOld(keep: Int = 30): Unit {
        return passDao.deleteOld(keep)
    }

    suspend fun getAll(): List<PassPopulated> {
        val passes = passDao.getAll()
        return passes.map { PassPopulated(it.pass) }
    }

    suspend fun setManySynced(passIds: List<String>, syncState: Boolean) {
        if (passIds.isEmpty()) return

        val passes = passDao.getAll()

        // Use forEach to update the 'synced' property
        passes.forEach { passes ->
            if (passIds.contains(passes.pass.id)) {
                passes.pass.synced = syncState
            }
        }

        // Update all modified cashups in the database
        passDao.updateMany(passes.map { it.pass })
    }

    suspend fun getById(id: String): PassEntity? {
        return passDao.getById(id)
    }

    suspend fun getManyById(passIds: List<String>): List<PassEntity> {
        var passes = passDao.getManyById(passIds)
        return passes
    }

    suspend fun getByOrderId(orderId: String): PassEntity? {
        return passDao.getByOrderId(orderId)
    }

    suspend fun getManyByOrderId(orderId: String): List<PassEntity> {
        return passDao.getManyByOrderId(orderId)
    }

    suspend fun registerPrinting(passIds: List<String>) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)
        var passes = passDao.getManyById(passIds)
        val popPasses = passDao.getManyPopulatedById(passIds)

        popPasses.forEach { popPass ->
            if (popPass.pass.isPrintingAllowed) {
                popPass.acquisitions.forEach { acquisition ->
                    authManager.incrementTicketReprintingAmount(acquisition.price)
                    authManager.incrementTicketReprintingCount(1L)
                }
            }
        }

        passes = passes.map {
            if (it.isPrintingAllowed) {
                it.printingRetries = it.printingRetries + 1
                it.synced = false
            }

            it
        }

        passDao.updateMany(passes)
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<PassEntity> {
        return passDao.getBySyncState(syncState) ?: emptyList()
    }

    suspend fun getAllByPrintingRetries(min: Int, max: Int): List<PassEntity> {
        return passDao.getAllByPrintingRetries(min, max)
    }

    suspend fun insertMany(passes: List<PassEntity>) {
        return passDao.insertMany(passes)
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: PassRepository? = null

        fun getInstance(passDao: PassDao) =
            instance ?: synchronized(this) {
                instance ?: PassRepository(passDao).also { instance = it }
            }
    }
}
