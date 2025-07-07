package br.com.ticpass.pos.data.room.repository

import br.com.ticpass.pos.data.room.dao.VoucherExchangeProductDao
import br.com.ticpass.pos.data.room.entity.VoucherExchangeProductPopulated
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoucherExchangeProductRepository @Inject constructor(
    private val voucherExchangeProductDao: VoucherExchangeProductDao
) {
    suspend fun deleteOld(keep: Int = 30): Unit {
        return voucherExchangeProductDao.deleteOld(keep)
    }

    suspend fun getAll(): List<VoucherExchangeProductPopulated> {
        val exchanges = voucherExchangeProductDao.getAll()
        return exchanges.map { VoucherExchangeProductPopulated(it.exchangeProduct) }
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: VoucherExchangeProductRepository? = null

        fun getInstance(voucherExchangeProductDao: VoucherExchangeProductDao) =
            instance ?: synchronized(this) {
                instance ?: VoucherExchangeProductRepository(voucherExchangeProductDao).also { instance = it }
            }
    }
}
