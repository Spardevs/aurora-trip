package br.com.ticpass.pos.data.room.repository


import br.com.ticpass.pos.data.room.dao.CashierDao
import br.com.ticpass.pos.data.room.entity.CashierEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashierRepository @Inject constructor(
    private val cashierDao: CashierDao
) {

    suspend fun insertUser(cashier: CashierEntity): Long {
        return cashierDao.insertUser(cashier)
    }

    suspend fun getUser() = cashierDao.getUser()

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: CashierRepository? = null

        fun getInstance(cashierDao: CashierDao) =
            instance ?: synchronized(this) {
                instance ?: CashierRepository(cashierDao).also { instance = it }
            }
    }
}
