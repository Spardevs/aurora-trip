package br.com.ticpass.pos.core.di

import br.com.ticpass.pos.domain.menu.repository.MenuRepository
import br.com.ticpass.pos.domain.menu.usecase.DownloadMenuLogoUseCase
import br.com.ticpass.pos.domain.menu.usecase.GetAllMenuLogoFilesUseCase
import br.com.ticpass.pos.domain.menu.usecase.GetMenuLogoFileUseCase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MenuLogoDomainModule {

    @Provides
    @Singleton
    fun provideDownloadMenuLogoUseCase(menuLogoRepository: MenuRepository): DownloadMenuLogoUseCase {
        return DownloadMenuLogoUseCase(menuLogoRepository)
    }

    @Provides
    @Singleton
    fun provideGetMenuLogoFileUseCase(menuLogoRepository: MenuRepository): GetMenuLogoFileUseCase {
        return GetMenuLogoFileUseCase(menuLogoRepository)
    }

    @Provides
    @Singleton
    fun provideGetAllMenuLogoFilesUseCase(menuLogoRepository: MenuRepository): GetAllMenuLogoFilesUseCase {
        return GetAllMenuLogoFilesUseCase(menuLogoRepository)
    }
}