package br.com.ticpass.pos.core.di

import android.content.Context
import br.com.ticpass.pos.data.menu.datasource.MenuRemoteDataSource
import br.com.ticpass.pos.data.menu.remote.service.MenuLogoService
import br.com.ticpass.pos.data.menu.repository.MenuRepositoryImpl
import br.com.ticpass.pos.domain.menu.repository.MenuRepository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MenuLogoDataModule {

    @Provides
    @Singleton
    fun provideMenuLogoApiService(retrofit: Retrofit): MenuLogoService {
        return retrofit.create(MenuLogoService::class.java)
    }

    @Provides
    @Singleton
    fun provideMenuRemoteDataSource(
        menuApiService: br.com.ticpass.pos.data.menu.remote.service.MenuApiService?, // se existir provider
        logoService: MenuLogoService
    ): MenuRemoteDataSource {
        // MenuRemoteDataSource aceita parâmetros nullable; aqui passamos os providers existentes
        return MenuRemoteDataSource(
            menuApiService,
            logoService
        )
    }

    @Provides
    @Singleton
    fun provideMenuRepository(
        remoteDataSource: MenuRemoteDataSource,
        localDataSource: br.com.ticpass.pos.data.menu.datasource.MenuLocalDataSource,
        @ApplicationContext context: Context
    ): MenuRepository {
        return MenuRepositoryImpl(
            remoteDataSource = remoteDataSource,
            logoRemoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            context = context
        )
    }
}