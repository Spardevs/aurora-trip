package br.com.ticpass.pos.core.di

import br.com.ticpass.pos.data.pos.datasource.PosLocalDataSource
import br.com.ticpass.pos.data.pos.datasource.PosRemoteDataSource
import br.com.ticpass.pos.data.pos.local.dao.PosDao
import br.com.ticpass.pos.data.pos.remote.service.PosApiService
import br.com.ticpass.pos.data.pos.repository.PosRepositoryImpl
import br.com.ticpass.pos.domain.pos.repository.PosRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PosDataModule {

    @Provides
    @Singleton
    fun providePosApiService(retrofit: Retrofit): PosApiService =
        retrofit.create(PosApiService::class.java)

    @Provides
    @Singleton
    fun providePosRemoteDataSource(api: PosApiService) = PosRemoteDataSource(api)

    @Provides
    @Singleton
    fun providePosLocalDataSource(dao: PosDao) = PosLocalDataSource(dao)

    @Provides
    @Singleton
    fun providePosRepository(
        remoteDataSource: PosRemoteDataSource,
        localDataSource: PosLocalDataSource
    ): PosRepository {
        return PosRepositoryImpl(remoteDataSource, localDataSource)
    }
}