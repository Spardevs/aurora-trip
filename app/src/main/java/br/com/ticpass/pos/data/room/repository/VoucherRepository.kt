package br.com.ticpass.pos.data.room.repository

import android.util.Log
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.dao.AcquisitionDao
import br.com.ticpass.pos.data.room.dao.VoucherDao
import br.com.ticpass.pos.data.room.entity.AcquisitionPopulated
import br.com.ticpass.pos.data.room.entity.VoucherEntity
import br.com.ticpass.pos.data.room.entity.VoucherPopulated
import br.com.ticpass.pos.data.room.entity.VoucherRedemptionEntity
import br.com.ticpass.pos.dataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoucherRepository @Inject constructor(
    private val voucherDao: VoucherDao,
    private val acquisitionDao: AcquisitionDao,
) {
    suspend fun deleteOld(keep: Int = 30): Unit {
        return voucherDao.deleteOld(keep)
    }

    suspend fun getManyById(ids: List<String>): List<VoucherPopulated> {
        val vouchers = voucherDao.getManyById(ids)
        return vouchers.map { VoucherPopulated(it.voucher, it.exchanges, it.redemptions) }
    }

    suspend fun insertMany(vouchers: List<VoucherEntity>) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)

        val ids = vouchers.map { it.id }
        val records = getManyById(ids)
        val notSaved = vouchers.filterNot { voucher -> records.any { record -> record?.id == voucher.id } }
        val popVouchers = records.filter { notSaved.any { voucher -> voucher?.id == it?.id } }

        popVouchers.forEach { popVoucher ->
            popVoucher.resources.forEach { resource ->
                authManager.incrementVoucherCount(resource.name, resource.price, 1)
            }
        }

        return voucherDao.insertMany(notSaved)
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<VoucherEntity> {
        return voucherDao.getBySyncState(syncState) ?: emptyList()
    }

    suspend fun getAll(): List<VoucherPopulated> {

        val vouchers = voucherDao.getAll()

        val populated = vouchers.map { voucher ->

            val exchanges = acquisitionDao.getAllByVoucherId(voucher.id)
            val redemptions = listOf<VoucherRedemptionEntity>()

            VoucherPopulated(
                voucher = voucher,
                resources = exchanges,
                redemptions = redemptions,
            )
        }

        Log.wtf("VoucherRepository", "getAll: $populated")

        return populated
    }

    suspend fun setManySynced(voucherIds: List<String>, syncState: Boolean) {
        if (voucherIds.isEmpty()) return

        val vouchers = voucherDao.getAll()

        // Use forEach to update the 'synced' property
        vouchers.forEach { voucher ->
            if (voucherIds.contains(voucher.id)) {
                voucher.synced = syncState
            }
        }

        voucherDao.updateMany(vouchers)
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: VoucherRepository? = null

        fun getInstance(voucherDao: VoucherDao, acquisitionDao: AcquisitionDao) =
            instance ?: synchronized(this) {
                instance ?: VoucherRepository(voucherDao, acquisitionDao).also { instance = it }
            }
    }
}
