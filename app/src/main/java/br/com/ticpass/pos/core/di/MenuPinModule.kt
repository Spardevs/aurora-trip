package br.com.ticpass.pos.core.di

import br.com.ticpass.pos.data.menupin.repository.MenuPinRepositoryImpl
import br.com.ticpass.pos.domain.menupin.repository.MenuPinRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MenuPinModule {

    @Binds
    abstract fun bindMenuPinRepository(
        impl: MenuPinRepositoryImpl
    ): MenuPinRepository
}
