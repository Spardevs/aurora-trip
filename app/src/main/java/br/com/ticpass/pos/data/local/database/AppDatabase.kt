package br.com.ticpass.pos.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.ticpass.pos.data.category.local.dao.CategoryDao
import br.com.ticpass.pos.data.category.local.entity.CategoryEntity
import br.com.ticpass.pos.data.menu.local.dao.MenuDao
import br.com.ticpass.pos.data.menu.local.entity.MenuEntity
import br.com.ticpass.pos.data.pos.local.dao.PosDao
import br.com.ticpass.pos.data.pos.local.entity.PosEntity
import br.com.ticpass.pos.data.product.local.dao.ProductDao
import br.com.ticpass.pos.data.product.local.entity.ProductEntity
import br.com.ticpass.pos.data.user.local.dao.UserDao
import br.com.ticpass.pos.data.user.local.entity.UserEntity
import br.com.ticpass.pos.core.queue.processors.payment.data.PaymentProcessingQueueDao
import br.com.ticpass.pos.core.queue.processors.payment.models.PaymentProcessingEntity
import br.com.ticpass.pos.core.queue.processors.nfc.data.NFCQueueDao
import br.com.ticpass.pos.core.queue.processors.nfc.models.NFCQueueEntity
import br.com.ticpass.pos.core.queue.processors.printing.data.PrintingQueueDao
import br.com.ticpass.pos.core.queue.processors.printing.models.PrintingEntity
import br.com.ticpass.pos.core.queue.processors.refund.data.RefundQueueDao
import br.com.ticpass.pos.core.queue.processors.refund.models.RefundQueueEntity

@Database(
    entities = [
        UserEntity::class, 
        MenuEntity::class, 
        PosEntity::class, 
        ProductEntity::class, 
        CategoryEntity::class,
        PaymentProcessingEntity::class,
        NFCQueueEntity::class,
        PrintingEntity::class,
        RefundQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)


abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun menuDao(): MenuDao
    abstract fun posDao(): PosDao
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    
    // Queue processor DAOs
    abstract fun paymentProcessingQueueDao(): PaymentProcessingQueueDao
    abstract fun nfcQueueDao(): NFCQueueDao
    abstract fun printingQueueDao(): PrintingQueueDao
    abstract fun refundQueueDao(): RefundQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ticpass_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}