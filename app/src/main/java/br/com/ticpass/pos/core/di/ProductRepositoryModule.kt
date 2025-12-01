package br.com.ticpass.pos.core.di

import br.com.ticpass.pos.data.product.repository.ProductRepositoryImpl
import br.com.ticpass.pos.domain.product.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindProductRepository(
        impl: ProductRepositoryImpl
    ): ProductRepository
}