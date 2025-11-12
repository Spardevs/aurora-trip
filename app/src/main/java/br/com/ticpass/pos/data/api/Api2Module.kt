package br.com.ticpass.pos.data.api

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Api2Module {

    @Provides
    @Singleton
    fun provideApi2Service(
        @ApplicationContext context: Context
    ): Api2Service {
        return Api2Service.create(context)
    }

    @Provides
    @Singleton
    fun provideApi2Repository(
        @ApplicationContext context: Context,
        service: Api2Service
    ): Api2Repository {
        return Api2Repository(context, service)
    }
}