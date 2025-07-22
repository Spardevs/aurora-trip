package br.com.ticpass.pos.queue.payment.di

import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueDao
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueFactory
import br.com.ticpass.pos.queue.payment.ProcessingPaymentStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideProcessingPaymentStorage(
        dao: ProcessingPaymentQueueDao
    ): ProcessingPaymentStorage {
        return ProcessingPaymentStorage(dao)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object PaymentQueueModule {

    @Provides
    @ViewModelScoped
    fun provideProcessingPaymentQueueFactory(): ProcessingPaymentQueueFactory {
        return ProcessingPaymentQueueFactory()
    }
}
