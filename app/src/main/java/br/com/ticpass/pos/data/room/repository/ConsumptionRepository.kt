package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.dao.AcquisitionDao
import br.com.ticpass.pos.data.room.dao.ConsumptionDao
import br.com.ticpass.pos.data.room.entity.ConsumptionEntity
import br.com.ticpass.pos.data.room.entity.ConsumptionPopulated
import br.com.ticpass.pos.dataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsumptionRepository @Inject constructor(
    private val consumptionDao: ConsumptionDao,
    private val acquisitionDao: AcquisitionDao,
) {
    suspend fun deleteOld(keep: Int = 30): Unit {
        return consumptionDao.deleteOld(keep)
    }

    suspend fun getManyById(ids: List<String>): List<ConsumptionPopulated> {
        val consumptions = consumptionDao.getManyById(ids)
        return consumptions.map { ConsumptionPopulated(it.consumption, it.resources) }
    }

    suspend fun insertMany(consumptions: List<ConsumptionEntity>) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)

        val ids = consumptions.map { it.id }
        val records = getManyById(ids)
        val notSaved = consumptions.filterNot { consumption -> records.any { record -> record?.id == consumption.id } }
        val popConsumptions = records.filter { notSaved.any { consumption -> consumption?.id == it?.id } }

        popConsumptions.forEach { popConsumption ->
            popConsumption.resources.forEach { resource ->
                authManager.incrementConsumptionCount(resource.name, resource.price, 1)
            }
        }

        return consumptionDao.insertMany(notSaved)
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<ConsumptionEntity> {
        return consumptionDao.getBySyncState(syncState) ?: emptyList()
    }

    suspend fun getAll(): List<ConsumptionPopulated> {

        val consumptions = consumptionDao.getAll()

        val populated = consumptions.map { consumption ->

            val exchanges = acquisitionDao.getAllByConsumptionId(consumption.id)

            ConsumptionPopulated(
                consumption = consumption,
                resources = exchanges,
            )
        }

        return populated
    }

    suspend fun setManySynced(consumptionIds: List<String>, syncState: Boolean) {
        if (consumptionIds.isEmpty()) return

        val consumptions = consumptionDao.getAll()

        // Use forEach to update the 'synced' property
        consumptions.forEach { consumption ->
            if (consumptionIds.contains(consumption.id)) {
                consumption.synced = syncState
            }
        }

        // Update all modified cashups in the database
        consumptionDao.updateMany(consumptions)
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: ConsumptionRepository? = null

        fun getInstance(consumptionDao: ConsumptionDao, acquisitionDao: AcquisitionDao) =
            instance ?: synchronized(this) {
                instance ?: ConsumptionRepository(consumptionDao, acquisitionDao).also { instance = it }
            }
    }
}
