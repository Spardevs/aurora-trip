package br.com.ticpass.pos.core.di

import br.com.ticpass.pos.data.pos.datasource.PosLocalDataSource
import br.com.ticpass.pos.data.pos.datasource.PosRemoteDataSource
import br.com.ticpass.pos.data.pos.repository.PosRepositoryImpl
import br.com.ticpass.pos.domain.pos.repository.PosRepository
import br.com.ticpass.pos.domain.pos.usecase.GetPosByMenuUseCase
import br.com.ticpass.pos.domain.pos.usecase.RefreshPosListUseCase
import org.koin.dsl.module

val posModule = module {

    // Data sources
    factory { PosRemoteDataSource(get()) }
    factory { PosLocalDataSource(get()) }

    // Use cases
    factory { GetPosByMenuUseCase(get()) }
    factory { RefreshPosListUseCase(get()) }

    // ViewModel
    factory<PosRepository> { PosRepositoryImpl(get(), get()) }
}