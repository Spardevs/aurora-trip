package br.com.ticpass.pos.data.acquirers.workers.di

import br.com.ticpass.pos.data.api.APIService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideAPIService(): APIService {
        return APIService.create()
    }
}
