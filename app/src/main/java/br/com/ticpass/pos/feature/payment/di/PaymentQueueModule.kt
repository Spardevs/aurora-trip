package br.com.ticpass.pos.feature.payment.di

import br.com.ticpass.pos.queue.processors.payment.data.ProcessingPaymentStorage
import br.com.ticpass.pos.queue.processors.payment.data.ProcessingPaymentQueueDao
import br.com.ticpass.pos.queue.processors.payment.utils.ProcessingPaymentQueueFactory
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
