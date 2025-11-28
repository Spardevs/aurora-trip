package br.com.ticpass.pos.core.network

import br.com.ticpass.Constants
import br.com.ticpass.pos.core.network.interceptor.AuthInterceptor
import br.com.ticpass.pos.data.auth.remote.service.AuthService
import br.com.ticpass.pos.data.device.remote.service.DeviceService
import br.com.ticpass.pos.data.menu.remote.service.MenuApiService
import br.com.ticpass.pos.data.menu.remote.service.MenuLogoService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.API_HOST)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideDeviceService(retrofit: Retrofit): DeviceService {
        return retrofit.create(DeviceService::class.java)
    }

    @Provides
    @Singleton
    fun provideMenuApiService(retrofit: Retrofit): MenuApiService {
        return retrofit.create(MenuApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMenuLogoService(retrofit: Retrofit): MenuLogoService {
        return retrofit.create(MenuLogoService::class.java)
    }
}