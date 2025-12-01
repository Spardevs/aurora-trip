package br.com.ticpass.pos.core.di

import br.com.ticpass.pos.data.category.repository.CategoryRepositoryImpl
import br.com.ticpass.pos.domain.category.repository.CategoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryRepositoryModule {

    @Binds
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository
}