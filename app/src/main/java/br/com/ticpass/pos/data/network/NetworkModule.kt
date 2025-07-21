package br.com.ticpass.pos.data.network

import android.content.Context
import br.com.ticpass.pos.data.api.APIService
import br.com.ticpass.pos.data.network.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(
        @ApplicationContext ctx: Context
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(ctx))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.ticpass.com.br/")
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()
                )
            )
            .build()

    @Singleton
    @Provides
    fun provideAPIService(
        @ApplicationContext ctx: Context
    ): APIService {
        return APIService.Companion.create(ctx)
    }
}