package br.com.ticpass.pos.data.room.repository

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.data.room.dao.PaymentDao
import br.com.ticpass.pos.data.room.entity.PaymentEntity
import br.com.ticpass.pos.dataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao
) {

    suspend fun insertMany(payments: List<PaymentEntity>) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)

        val ids = payments.map { it.id }
        val records = paymentDao.getManyByIds(ids)
        val notSaved = payments.filterNot { payment -> records.any { record -> record?.id == payment.id } }

        notSaved.forEach { payment ->
            authManager.incrementPaymentIncome(payment.type, payment.amount)
        }

        return paymentDao.insertPayments(notSaved)
    }

    suspend fun getAllBySyncState(syncState: Boolean): List<PaymentEntity> {
        return paymentDao.getBySyncState(syncState) ?: emptyList()
    }

    suspend fun getById(id: String): PaymentEntity? {
        val records = paymentDao.getManyByIds(listOf(id))
        return records.firstOrNull()
    }

    suspend fun deleteOld(keep: Int = 30): Unit {
        return paymentDao.deleteOld(keep)
    }

    suspend fun sumCommission(): Long {
        return paymentDao.sumCommission()
    }

    suspend fun delete(): Long {
        return paymentDao.sumCommission()
    }

    suspend fun sumAmountsByType(paymentType: String): Long {
        return paymentDao.sumAmountsByType(paymentType)
    }

    suspend fun getAll() = paymentDao.getAll()

    suspend fun setManySynced(paymentIds: List<String>, syncState: Boolean) {
        if (paymentIds.isEmpty()) return

        val payments = paymentDao.getAll()

        // Use forEach to update the 'synced' property
        payments.forEach { payment ->
            if (paymentIds.contains(payment.id)) {
                payment.synced = syncState
            }
        }

        // Update all modified cashups in the database
        paymentDao.updateMany(payments)
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: PaymentRepository? = null

        fun getInstance(paymentDao: PaymentDao) =
            instance ?: synchronized(this) {
                instance ?: PaymentRepository(paymentDao).also { instance = it }
            }
    }

    suspend fun insertPayment(payment: PaymentEntity) {
        val authManager = AuthManager(MainActivity.appContext.dataStore)
        val existingPayment = paymentDao.getPaymentById(payment.id)
        if (existingPayment == null) {
            authManager.incrementPaymentIncome(payment.type, payment.amount)
            paymentDao.insertPayment(payment)
        } else {
            paymentDao.insertPayment(payment)
        }
    }
}
