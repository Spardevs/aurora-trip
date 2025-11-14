package br.com.ticpass.pos.data.api

import android.content.Context
import android.util.Log
import br.com.ticpass.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

interface Api2Service {

    @POST("auth/signin/pos/short-lived")
    @Headers("Content-Type: application/json")
    suspend fun signInShortLived(
        @retrofit2.http.Header("Cookie") cookie: String,
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: okhttp3.RequestBody
    ): Response<LoginResponse>

    @POST("auth/signin/pos")
    @Headers("Content-Type: application/json")
    suspend fun signInWithEmailPassword(
        @Body body: okhttp3.RequestBody
    ): Response<LoginResponse>

    @POST("devices")
    @Headers("Content-Type: application/json")
    suspend fun registerDevice(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: okhttp3.RequestBody
    ): Response<RegisterDeviceResponse>

    @GET("menu")
    @Headers("Content-Type: application/json")
    suspend fun getMenu(
        @Query("take") take: Int = 10,
        @Query("page") page: Int = 1
    ): Response<MenuListResponse>

    @GET("menu/logo/{menuId}/download")
    @Streaming
    suspend fun downloadMenuLogo(
        @Path("menuId") menuId: String,
        @retrofit2.http.Header("X-Use-Access-Token") useAccessToken: Boolean = true
    ): Response<ResponseBody>

    @GET("menu-pos")
    @Headers("Content-Type: application/json")
    suspend fun getMenuPos(
        @Query("take") take: Int = 10,
        @Query("page") page: Int = 1,
        @Query("menu") menu: String,
        @Query("available") available: String = "both"
    ): Response<MenuPosListResponse>

    companion object {
        private var BASE_URL = "${Constants.API_HOST}/"

        @JvmStatic
        fun create(context: Context): Api2Service {
            val logger = HttpLoggingInterceptor().apply { level = Level.BODY }

            val client = OkHttpClient.Builder()
                .connectTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(VersionInterceptor()) // ✅ Header version: 2.0.0
                .addInterceptor(ApiAuthInterceptor(context)) // ✅ Headers de auth
                .addInterceptor(logger)
                .build()

            Log.d("Api2Service", "Creating Retrofit @ $BASE_URL")

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Api2Service::class.java)
        }
    }
}