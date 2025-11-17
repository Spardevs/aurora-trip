package br.com.ticpass.pos.data.api

import android.content.Context
import android.util.Log
import br.com.ticpass.Constants
import br.com.ticpass.pos.data.network.interceptor.ApiAuthInterceptor
import br.com.ticpass.pos.data.network.interceptor.VersionInterceptor
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
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

interface ApiService {

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

    @POST("menu-pos-sessions/open")
    @Headers("Content-Type: application/json")
    suspend fun openPosSession(
        @retrofit2.http.Header("Cookie") cookie: String,
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: okhttp3.RequestBody
    ): Response<OpenPosSessionResponse>

    @GET("menu-pos-sessions/open")
    @Headers("Content-Type: application/json")
    suspend fun getPosSessionProducts(
        @retrofit2.http.Header("Cookie") cookie: String,
        @retrofit2.http.Header("Authorization") authorization: String
    ): Response<PosSessionProductsResponse>

    @GET("menu/{menuId}/product/thumbnail/download/all")
    @Streaming
    suspend fun downloadAllProductThumbnails(
        @Path("menuId") menuId: String,
        @retrofit2.http.Header("Cookie") cookie: String,
        @retrofit2.http.Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @PUT("menu-pos-sessions/close")
    @Headers("Content-Type: application/json")
    suspend fun closePosSession(
        @retrofit2.http.Header("Cookie") cookie: String,
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: okhttp3.RequestBody
    ): Response<ClosePosSessionResponse>

    @POST("device-locations/ping")
    @Headers("Content-Type: application/json")
    suspend fun pingDeviceLocation(
        @retrofit2.http.Header("Cookie") cookie: String,
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: okhttp3.RequestBody
    ): Response<DevicePingResponse>

    @POST("menu-pos-sessions/{sessionId}/sync")
    @Headers("Content-Type: application/json")
    suspend fun syncMenuPosSession(
        @Path("sessionId") sessionId: String,
        @retrofit2.http.Header("Cookie") cookie: String,
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: okhttp3.RequestBody
    ): Response<SyncMenuPosSessionResponse>

    @POST("auth/refresh")
    @Headers("Content-Type: application/json")
    suspend fun refreshToken(
        @retrofit2.http.Header("Cookie") cookie: String,
        @retrofit2.http.Header("Authorization") authorization: String
    ): Response<RefreshTokenResponse>

    companion object {
        private var BASE_URL = "${Constants.API_HOST}/"

        @JvmStatic
        fun create(context: Context): ApiService {
            val logger = HttpLoggingInterceptor().apply { level = Level.BODY }

            // Cria manualmente um TokenManager para uso local (evita ciclos de dependência)
            val tokenManager = br.com.ticpass.pos.data.network.TokenManager(context)
            val authInterceptor = br.com.ticpass.pos.data.network.interceptor.ApiAuthInterceptor(tokenManager)

            val client = OkHttpClient.Builder()
                .connectTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(VersionInterceptor()) // header version
                .addInterceptor(authInterceptor) // headers auth (agora recebe TokenManager)
                .addInterceptor(logger)
                .build()

            Log.d("Api2Service", "Creating Retrofit @ $BASE_URL")

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}