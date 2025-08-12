package br.com.ticpass.pos.feature.printing.di

import br.com.ticpass.pos.queue.processors.printing.data.PrintingStorage
import br.com.ticpass.pos.queue.processors.printing.data.PrintingQueueDao
import br.com.ticpass.pos.queue.processors.printing.utils.PrintingQueueFactory
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
    fun providePrintingStorage(
        dao: PrintingQueueDao
    ): PrintingStorage {
        return PrintingStorage(dao)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object PrintingQueueModule {

    @Provides
    @ViewModelScoped
    fun providePrintingQueueFactory(): PrintingQueueFactory {
        return PrintingQueueFactory()
    }
}
