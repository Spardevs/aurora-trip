package br.com.ticpass.pos.core.network

import android.content.Context
import br.com.ticpass.Constants
import br.com.ticpass.pos.core.network.interceptor.AuthInterceptor
import br.com.ticpass.pos.core.network.interceptor.RateLimitInterceptor
import br.com.ticpass.pos.core.network.interceptor.VersionInterceptor
import br.com.ticpass.pos.core.network.ratelimit.ExponentialBackoff
import br.com.ticpass.pos.core.network.ratelimit.RateLimiter
import br.com.ticpass.pos.core.network.ratelimit.RateLimitStorage
import br.com.ticpass.pos.data.auth.remote.service.AuthService
import br.com.ticpass.pos.data.category.remote.service.CategoryApiService
import br.com.ticpass.pos.data.device.remote.service.DeviceService
import br.com.ticpass.pos.data.menu.remote.service.MenuApiService
import br.com.ticpass.pos.data.menu.remote.service.MenuLogoService
import br.com.ticpass.pos.data.menupin.remote.service.MenuPinApiService
import br.com.ticpass.pos.data.product.remote.service.ProductApiService
import br.com.ticpass.pos.data.pos.remote.adapters.CashierDtoJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
    fun provideRateLimitStorage(@ApplicationContext context: Context): RateLimitStorage {
        return RateLimitStorage(context)
    }

    @Provides
    @Singleton
    fun provideExponentialBackoff(): ExponentialBackoff {
        return ExponentialBackoff()
    }

    @Provides
    @Singleton
    fun provideRateLimiter(
        rateLimitStorage: RateLimitStorage,
        exponentialBackoff: ExponentialBackoff
    ): RateLimiter {
        return RateLimiter(rateLimitStorage, exponentialBackoff)
    }

    @Provides
    @Singleton
    fun provideRateLimitInterceptor(rateLimiter: RateLimiter): Interceptor {
        return RateLimitInterceptor(rateLimiter)
    }
    @Provides
    @Singleton
    fun provideVersionInterceptor(): Interceptor {
        return VersionInterceptor()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        versionInterceptor: VersionInterceptor,
        rateLimitInterceptor: RateLimitInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(versionInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(CashierDtoJsonAdapterFactory)
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
    fun provideProductApiService(retrofit: Retrofit): ProductApiService {
        return retrofit.create(ProductApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCategoryApiService(retrofit: Retrofit): CategoryApiService {
        return retrofit.create(CategoryApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMenuLogoService(retrofit: Retrofit): MenuLogoService {
        return retrofit.create(MenuLogoService::class.java)
    }

    @Provides
    @Singleton
    fun provideMenuPinApiService(retrofit: Retrofit): MenuPinApiService {
        return retrofit.create(MenuPinApiService::class.java)
    }
}