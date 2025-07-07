package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.data.room.dao.VoucherRedemptionDao
import br.com.ticpass.pos.data.room.entity.VoucherRedemptionEntity
import br.com.ticpass.pos.data.room.entity.VoucherRedemptionPopulated
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoucherRedemptionRepository @Inject constructor(
    private val voucherRedemptionDao: VoucherRedemptionDao
) {
    suspend fun deleteOld(keep: Int = 30): Unit {
        return voucherRedemptionDao.deleteOld(keep)
    }

    suspend fun getAll(): List<VoucherRedemptionPopulated> {
        val redemptions = voucherRedemptionDao.getAll()
        return redemptions.map { VoucherRedemptionPopulated(it.redemption) }
    }

    suspend fun setManySynced(voucherRedemptionIds: List<String>, syncState: Boolean) {
        if (voucherRedemptionIds.isEmpty()) return

        val voucherRedemptions = voucherRedemptionDao.getAll()

        // Use forEach to update the 'synced' property
        voucherRedemptions.forEach { voucherRedemption ->
            if (voucherRedemptionIds.contains(voucherRedemption.redemption.id)) {
                voucherRedemption.redemption.synced = syncState
            }
        }

        voucherRedemptionDao.updateMany(voucherRedemptions.map { it.redemption })
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<VoucherRedemptionEntity> {
        return voucherRedemptionDao.getBySyncState(syncState) ?: emptyList()
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: VoucherRedemptionRepository? = null

        fun getInstance(voucherRedemptionDao: VoucherRedemptionDao) =
            instance ?: synchronized(this) {
                instance ?: VoucherRedemptionRepository(voucherRedemptionDao).also { instance = it }
            }
    }
}
