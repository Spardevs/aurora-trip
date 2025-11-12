package br.com.ticpass.pos.data.api

import android.content.Context
import android.util.Log
import br.com.ticpass.Constants
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Modelo de resposta para signin short-lived
 */
data class ShortLivedSignInResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("result") val result: ShortLivedSignInResult?,
    @SerializedName("error") val error: String?,
    @SerializedName("name") val name: String?
)

data class ShortLivedSignInResult(
    @SerializedName("token") val token: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("expiresIn") val expiresIn: Long?
)

/**
 * API Service para autenticação short-lived
 */
interface Api2Service {

    @POST("auth/signin/pos/short-lived")
    @Headers("Content-Type: application/json")
    suspend fun signInShortLived(
        @Header("Cookie") cookie: String,
        @Header("Authorization") authorization: String,
        @Body body: okhttp3.RequestBody
    ): ShortLivedSignInResponse

    companion object {
        private var BASE_URL = "${Constants.API_HOST}/"

        @JvmStatic
        fun create(context: Context): Api2Service {
            val logger = HttpLoggingInterceptor().apply { level = Level.BODY }

            val client = OkHttpClient.Builder()
                .connectTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(VersionInterceptor())
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