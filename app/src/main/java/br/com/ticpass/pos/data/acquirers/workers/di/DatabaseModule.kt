package br.com.ticpass.pos.data.acquirers.workers.di

import android.content.Context
import br.com.ticpass.pos.data.room.AppDatabase
import br.com.ticpass.pos.data.room.dao.AcquisitionDao
import br.com.ticpass.pos.data.room.dao.CartOrderLineDao
import br.com.ticpass.pos.data.room.dao.CashupDao
import br.com.ticpass.pos.data.room.dao.CategoryDao
import br.com.ticpass.pos.data.room.dao.ConsumptionDao
import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.dao.OrderDao
import br.com.ticpass.pos.data.room.dao.PassDao
import br.com.ticpass.pos.data.room.dao.PaymentDao
import br.com.ticpass.pos.data.room.dao.PosDao
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.dao.RefundDao
import br.com.ticpass.pos.data.room.dao.CashierDao
import br.com.ticpass.pos.data.room.dao.VoucherDao
import br.com.ticpass.pos.data.room.dao.VoucherRedemptionDao
import br.com.ticpass.pos.queue.processors.payment.data.PaymentProcessingQueueDao
import br.com.ticpass.pos.queue.processors.printing.data.PrintingQueueDao
import br.com.ticpass.pos.queue.processors.refund.data.RefundQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }


    @Provides
    fun provideCashierDao(appDatabase: AppDatabase): CashierDao {
        return appDatabase.cashierDao()
    }

    @Provides
    fun provideEventDao(appDatabase: AppDatabase): EventDao {
        return appDatabase.eventDao()
    }

    @Provides
    fun providePosDao(appDatabase: AppDatabase): PosDao {
        return appDatabase.posDao()
    }

    @Provides
    fun provideProductDao(appDatabase: AppDatabase): ProductDao {
        return appDatabase.productDao()
    }

    @Provides
    fun provideCategoryDao(appDatabase: AppDatabase): CategoryDao {
        return appDatabase.categoryDao()
    }

    @Provides
    fun provideOrderDao(appDatabase: AppDatabase): OrderDao {
        return appDatabase.orderDao()
    }

    @Provides
    fun providePaymentDao(appDatabase: AppDatabase): PaymentDao {
        return appDatabase.paymentDao()
    }

    @Provides
    fun provideCashupDao(appDatabase: AppDatabase): CashupDao {
        return appDatabase.cashupDao()
    }

    @Provides
    fun provideVoucherDao(appDatabase: AppDatabase): VoucherDao {
        return appDatabase.voucherDao()
    }

    @Provides
    fun provideVoucherRedemptionDao(appDatabase: AppDatabase): VoucherRedemptionDao {
        return appDatabase.voucherRedemptionDao()
    }

    @Provides
    fun provideRefundDao(appDatabase: AppDatabase): RefundDao {
        return appDatabase.refundDao()
    }

    @Provides
    fun provideAcquisitionDao(appDatabase: AppDatabase): AcquisitionDao {
        return appDatabase.acquisitionDao()
    }

    @Provides
    fun provideConsumptionDao(appDatabase: AppDatabase): ConsumptionDao {
        return appDatabase.consumptionDao()
    }

    @Provides
    fun providePassDao(appDatabase: AppDatabase): PassDao {
        return appDatabase.passDao()
    }

    @Provides
    fun provideCartOrderLineDao(appDatabase: AppDatabase): CartOrderLineDao {
        return appDatabase.cartOrderLineDao()
    }

    @Provides
    fun providePaymentProcessingQueueDao(appDatabase: AppDatabase): PaymentProcessingQueueDao {
        return appDatabase.processingPaymentQueueDao()
    }

    @Provides
    fun providePrintQueueDao(appDatabase: AppDatabase): PrintingQueueDao {
        return appDatabase.printingQueueDao()
    }

    @Provides
    fun provideRefundQueueDao(appDatabase: AppDatabase): RefundQueueDao {
        return appDatabase.refundQueueDao()
    }
}

