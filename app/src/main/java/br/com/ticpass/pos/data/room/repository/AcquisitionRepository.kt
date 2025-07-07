package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.dao.AcquisitionDao
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity
import br.com.ticpass.pos.data.room.entity.AcquisitionPopulated
import br.com.ticpass.pos.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcquisitionRepository @Inject constructor(
    private val acquisitionDao: AcquisitionDao
) {
    suspend fun deleteOld(keep: Int = 30): Unit {
        return acquisitionDao.deleteOld(keep)
    }

    suspend fun getManyById(ids: List<String>): List<AcquisitionPopulated> {
        val acquisitions = acquisitionDao.getManyById(ids)
        return acquisitions.map { AcquisitionPopulated(it.acquisition) }
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<AcquisitionEntity> {
        return acquisitionDao.getBySyncState(syncState) ?: emptyList()
    }

    suspend fun getManyByPassId(ids: List<String>): List<AcquisitionEntity> {
        return acquisitionDao.getManyByPassId(ids)
    }

    suspend fun getAll(): List<AcquisitionPopulated> {
        val acquisitions = acquisitionDao.getAll()
        return acquisitions.map { AcquisitionPopulated(it.acquisition) }
    }

    suspend fun setManySynced(acquisitionIds: List<String>, syncState: Boolean) {
        if (acquisitionIds.isEmpty()) return

        val acquisitions = acquisitionDao.getManyById(acquisitionIds)

        acquisitions.forEach { acquisition ->
            acquisition.acquisition.synced = syncState
            acquisitionDao.updateMany(acquisitions.map { it.acquisition })
        }
    }

    fun getAllAvailable(): List<AcquisitionEntity> {
        return acquisitionDao.getAllAvailable()
    }

    fun getAllRefunds(): List<AcquisitionEntity> {
        return acquisitionDao.getAllRefunds()
    }

    fun getAllVouchered(): List<AcquisitionEntity> {
        return acquisitionDao.getAllVouchered()
    }

    fun getAllConsumed(): List<AcquisitionEntity> {
        return acquisitionDao.getAllConsumed()
    }

    suspend fun getByOrderId(orderId: String): List<AcquisitionPopulated> {
        val acquisitions = acquisitionDao.getByOrderId(orderId)
        return acquisitions.map { AcquisitionPopulated(it.acquisition) }
    }

    suspend fun getUnconsumedByOrderId(orderId: String): List<AcquisitionEntity> =
        withContext(Dispatchers.IO) {
            return@withContext acquisitionDao.getUnconsumedByOrderId(orderId)
        }

    suspend fun getConsumedByOrderId(orderId: String): List<AcquisitionPopulated> {
        val acquisitions = acquisitionDao.getConsumedByOrderId(orderId)
        return acquisitions.map { AcquisitionPopulated(it.acquisition) }
    }

    suspend fun refundManyById(ids: List<String>, refundId: String) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)
        val acquisitions = getManyById(ids)

        val updated = acquisitions.map {
            val acquisition = AcquisitionEntity(
                id = it.id,
                pos = it.pos,
                event = it.event,
                pass = it.pass,
                order = it.order,
                product = it.product,
                name = it.name,
                logo = it.logo,
                price = it.price,
                category = it.category,
                createdAt = it.createdAt,
                commission = it.commission,
            )

            acquisition.refund = refundId
            acquisition.synced = false

            authManager.incrementRefund(it.name, it.price, 1)

            acquisition
        }

        acquisitionDao.updateMany(updated)
    }

    suspend fun voucherManyById(ids: List<String>, voucherId: String) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)
        val acquisitions = getManyById(ids)

        val updated = acquisitions.map {
            val acquisition = AcquisitionEntity(
                id = it.id,
                pos = it.pos,
                event = it.event,
                pass = it.pass,
                order = it.order,
                product = it.product,
                name = it.name,
                logo = it.logo,
                price = it.price,
                category = it.category,
                createdAt = it.createdAt,
                commission = it.commission,
            )

            acquisition.voucher = voucherId
            acquisition.synced = false

            authManager.incrementVoucherCount(it.name, it.price, 1)

            acquisition
        }

        acquisitionDao.updateMany(updated)
    }

    suspend fun consumeManyById(ids: List<String>, consumptionId: String) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)
        val acquisitions = getManyById(ids)

        val updated = acquisitions.map {
            val acquisition = AcquisitionEntity(
                id = it.id,
                pos = it.pos,
                event = it.event,
                pass = it.pass,
                order = it.order,
                product = it.product,
                name = it.name,
                logo = it.logo,
                price = it.price,
                category = it.category,
                createdAt = it.createdAt,
                commission = it.commission,
            )

            acquisition.consumption = consumptionId
            acquisition.synced = false

            authManager.incrementConsumptionCount(it.name, it.price, 1)

            acquisition
        }

        acquisitionDao.updateMany(updated)
    }

    suspend fun insertMany(acquisitions: List<AcquisitionEntity>) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)

        val ids = acquisitions.map { it.id }
        val records = getManyById(ids)
        val notSaved = acquisitions.filterNot { acquisition -> records.any { record -> record?.id == acquisition.id } }

        notSaved.forEach { acquisition ->
            authManager.incrementAcquisitionCount(acquisition.name, acquisition.price, 1)
            authManager.incrementCommission((acquisition.price * acquisition.commission) / 100L)
        }

        return acquisitionDao.insertMany(acquisitions)
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: AcquisitionRepository? = null

        fun getInstance(acquisitionDao: AcquisitionDao) =
            instance ?: synchronized(this) {
                instance ?: AcquisitionRepository(acquisitionDao).also { instance = it }
            }
    }
}
