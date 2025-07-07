package br.com.ticpass.pos.data.room.repository


import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.dao.AcquisitionDao
import br.com.ticpass.pos.data.room.dao.CashupDao
import br.com.ticpass.pos.data.room.dao.OrderDao
import br.com.ticpass.pos.data.room.entity.CashupEntity
import br.com.ticpass.pos.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashupRepository @Inject constructor(
    private val cashupDao: CashupDao,
    private val orderDao: OrderDao,
    private val acquisitionDao: AcquisitionDao,
) {

    suspend fun insertMany(cashups: List<CashupEntity>) {
        return cashupDao.insertCashups(cashups)
    }

    suspend fun getAll() = cashupDao.getAllCashups()

    suspend fun getTaken(): Long = withContext(Dispatchers.IO) {
        return@withContext cashupDao.getTaken()
    }

    suspend fun deleteOld(keep: Int = 30): Unit {
        return cashupDao.deleteOld(keep)
    }

    suspend fun getPosBalance(): Long = withContext(Dispatchers.IO) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)

        val revenue = authManager.getTotalAcquisition()
        val takenSoFar = authManager.getCashup()
        val refund = authManager.getTotalRefund()

        return@withContext revenue - (takenSoFar + refund)
    }

    suspend fun take(
        cashier: String,
        amount: Long,
    ) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)

        withContext(Dispatchers.IO) {
            val balance = getPosBalance()
            val remaining = balance - amount

            // Ensure we don't take more than available funds or negative amounts
            if (amount > balance || amount < 0) {
                return@withContext
            }

            authManager.incrementCashup(amount)

            cashupDao.insertCashups(
                listOf(
                    CashupEntity(
                        accountable = cashier,
                        createdAt = getCurrentDateString(),
                        initial = balance,
                        taken = amount,
                        remaining = remaining,
                    )
                )
            )
        }
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<CashupEntity> {
        return cashupDao.getBySyncState(syncState) ?: emptyList()
    }

    suspend fun setManySynced(cashupIds: List<String>, syncState: Boolean) {
        if (cashupIds.isEmpty()) return

        val cashups = cashupDao.getAllCashups() ?: emptyList()

        // Use forEach to update the 'synced' property
        cashups.forEach { cashup ->
            if (cashupIds.contains(cashup.id)) {
                cashup.synced = syncState
            }
        }

        // Update all modified cashups in the database
        cashupDao.updateMany(cashups)
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: CashupRepository? = null

        fun getInstance(cashupDao: CashupDao, orderDao: OrderDao, acquisitionDao: AcquisitionDao) =
            instance ?: synchronized(this) {
                instance ?: CashupRepository(cashupDao, orderDao, acquisitionDao).also { instance = it }
            }
    }
}
