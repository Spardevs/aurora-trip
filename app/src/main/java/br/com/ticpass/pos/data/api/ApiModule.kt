package br.com.ticpass.pos.data.api

import android.content.Context
import br.com.ticpass.pos.data.network.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideApiRepository(
        @ApplicationContext context: Context,
        service: ApiService,
        tokenManager: TokenManager
    ): ApiRepository {
        return ApiRepository(context, service, tokenManager)
    }
}