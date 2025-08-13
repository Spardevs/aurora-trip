package br.com.ticpass.pos.feature.refund.di

import br.com.ticpass.pos.queue.processors.refund.data.RefundStorage
import br.com.ticpass.pos.queue.processors.refund.data.RefundQueueDao
import br.com.ticpass.pos.queue.processors.refund.utils.RefundQueueFactory
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
    fun provideRefundStorage(
        dao: RefundQueueDao
    ): RefundStorage {
        return RefundStorage(dao)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object RefundQueueModule {

    @Provides
    @ViewModelScoped
    fun provideRefundQueueFactory(): RefundQueueFactory {
        return RefundQueueFactory()
    }
}
