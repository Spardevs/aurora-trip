package br.com.ticpass.pos.core.di

import android.content.Context
import androidx.room.Room
import br.com.ticpass.pos.data.category.local.dao.CategoryDao
import br.com.ticpass.pos.data.local.database.AppDatabase
import br.com.ticpass.pos.data.menu.local.dao.MenuDao
import br.com.ticpass.pos.data.pos.local.dao.PosDao
import br.com.ticpass.pos.data.product.local.dao.ProductDao
import br.com.ticpass.pos.data.user.local.dao.UserDao
import br.com.ticpass.pos.core.queue.processors.payment.data.PaymentProcessingQueueDao
import br.com.ticpass.pos.core.queue.processors.nfc.data.NFCQueueDao
import br.com.ticpass.pos.core.queue.processors.printing.data.PrintingQueueDao
import br.com.ticpass.pos.core.queue.processors.refund.data.RefundQueueDao
import br.com.ticpass.pos.data.menupin.local.dao.MenuPinDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ticpass_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePosDao(appDatabase: AppDatabase): PosDao {
        return appDatabase.posDao()
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    fun provideMenuDao(appDatabase: AppDatabase): MenuDao {
        return appDatabase.menuDao()
    }

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    // Queue processor DAOs
    @Provides
    fun providePaymentProcessingQueueDao(database: AppDatabase): PaymentProcessingQueueDao {
        return database.paymentProcessingQueueDao()
    }

    @Provides
    fun provideNFCQueueDao(database: AppDatabase): NFCQueueDao {
        return database.nfcQueueDao()
    }

    @Provides
    fun providePrintingQueueDao(database: AppDatabase): PrintingQueueDao {
        return database.printingQueueDao()
    }

    @Provides
    fun provideRefundQueueDao(database: AppDatabase): RefundQueueDao {
        return database.refundQueueDao()
    }

    @Provides
    fun provideMenuPinDao(database: AppDatabase): MenuPinDao {
        return database.menuPinDao()
    }
}