package br.com.ticpass.pos.queue.payment.di

import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueDao
import br.com.ticpass.pos.queue.payment.ProcessingPaymentStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PaymentQueueModule {

    @Provides
    @Singleton
    fun provideProcessingPaymentStorage(
        dao: ProcessingPaymentQueueDao
    ): ProcessingPaymentStorage {
        return ProcessingPaymentStorage(dao)
    }
}
