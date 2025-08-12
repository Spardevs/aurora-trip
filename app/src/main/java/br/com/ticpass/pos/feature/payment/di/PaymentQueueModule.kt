package br.com.ticpass.pos.feature.payment.di

import br.com.ticpass.pos.queue.processors.payment.data.PaymentProcessingStorage
import br.com.ticpass.pos.queue.processors.payment.data.PaymentProcessingQueueDao
import br.com.ticpass.pos.queue.processors.payment.utils.PaymentProcessingQueueFactory
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
    fun providePaymentProcessingStorage(
        dao: PaymentProcessingQueueDao
    ): PaymentProcessingStorage {
        return PaymentProcessingStorage(dao)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object PaymentQueueModule {

    @Provides
    @ViewModelScoped
    fun providePaymentProcessingQueueFactory(): PaymentProcessingQueueFactory {
        return PaymentProcessingQueueFactory()
    }
}
