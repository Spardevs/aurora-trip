package br.com.ticpass.pos.data.room.repository


import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.dao.AcquisitionDao
import br.com.ticpass.pos.data.room.dao.RefundDao
import br.com.ticpass.pos.data.room.entity.RefundEntity
import br.com.ticpass.pos.data.room.entity.RefundPopulated
import br.com.ticpass.pos.data.room.entity.VoucherEntity
import br.com.ticpass.pos.data.room.entity.VoucherPopulated
import br.com.ticpass.pos.dataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefundRepository @Inject constructor(
    private val refundDao: RefundDao,
    private val acquisitionDao: AcquisitionDao,
) {
    suspend fun deleteOld(keep: Int = 30): Unit {
        return refundDao.deleteOld(keep)
    }

    suspend fun getManyById(ids: List<String>): List<RefundPopulated> {
        val refunds = refundDao.getManyById(ids)
        return refunds.map { RefundPopulated(it.refund, it.exchanges) }
    }

    suspend fun insertMany(refunds: List<RefundEntity>) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)

        val ids = refunds.map { it.id }
        val records = getManyById(ids)
        val notSaved = refunds.filterNot { refund -> records.any { record -> record?.id == refund.id } }
        val popRefunds = records.filter { notSaved.any { refund -> refund?.id == it?.id } }

        popRefunds.forEach { popRefund ->
            popRefund.resources.forEach { resource ->
                authManager.incrementRefund(resource.name, resource.price, 1)
            }
        }

        return refundDao.insertMany(notSaved)
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<RefundEntity> {
        return refundDao.getBySyncState(syncState) ?: emptyList()
    }

    suspend fun getAll(): List<RefundPopulated> {

        val refunds = refundDao.getAll()

        val populated = refunds.map { refund ->

            val resources = acquisitionDao.getAllByRefundId(refund.id)

            RefundPopulated(
                refund = refund,
                resources = resources,
            )
        }

        return populated
    }

    suspend fun setManySynced(refundIds: List<String>, syncState: Boolean) {
        if (refundIds.isEmpty()) return

        val refunds = refundDao.getAll()

        // Use forEach to update the 'synced' property
        refunds.forEach { refund ->
            if (refundIds.contains(refund.id)) {
                refund.synced = syncState
            }
        }

        refundDao.updateMany(refunds)
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: RefundRepository? = null

        fun getInstance(refundDao: RefundDao, acquisitionDao: AcquisitionDao) =
            instance ?: synchronized(this) {
                instance ?: RefundRepository(refundDao, acquisitionDao).also { instance = it }
            }
    }
}
