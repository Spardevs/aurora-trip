package br.com.ticpass.pos.feature.nfc.di

import br.com.ticpass.pos.queue.processors.nfc.data.NFCStorage
import br.com.ticpass.pos.queue.processors.nfc.data.NFCQueueDao
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCQueueFactory
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
    fun provideNFCStorage(
        dao: NFCQueueDao
    ): NFCStorage {
        return NFCStorage(dao)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object NFCQueueModule {

    @Provides
    @ViewModelScoped
    fun provideNFCQueueFactory(): NFCQueueFactory {
        return NFCQueueFactory()
    }
}
