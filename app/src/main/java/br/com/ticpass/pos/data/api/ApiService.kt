
package br.com.ticpass.pos.data.api

import android.util.Log
import br.com.ticpass.Constants.API_HOST
import br.com.ticpass.Constants.API_TIMEOUT_SECONDS
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


data class PaymentProductPostData(
    @SerializedName("fkProduct") val productId: Int,
    @SerializedName("amount") val count: Long,
    @SerializedName("totalValue") val price: Long
)

data class SyncPosPostData(
    @SerializedName("orders") val orders: List<SyncPosOrderPostData>,
    @SerializedName("payments") val payments: List<SyncPosPaymentPostData>,
    @SerializedName("passes") val passes: List<SyncPosPassPostData>,
    @SerializedName("acquisitions") val acquisitions: List<SyncPosAcquisitionPostData>,
    @SerializedName("refunds") val refunds: List<SyncPosRefundPostData>,
    @SerializedName("consumptions") val consumptions: List<SyncPosConsumptionPostData>,
    @SerializedName("vouchers") val vouchers: List<SyncPosVoucherPostData>,
    @SerializedName("voucherRedemptions") val voucherRedemptions: List<SyncPosVoucherRedemptionPostData>,
    @SerializedName("cashups") val cashups: List<SyncPosCashupPostData>,
)

data class SyncPosOrderPostData(
    @SerializedName("id") val id: String,
    @SerializedName("coords") val coords: String?,
    @SerializedName("createdAt") var createdAt: String,
)

data class SyncPosPaymentPostData(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("createdAt") var createdAt: String,
    @SerializedName("amount") var amount: Long,
    @SerializedName("commission") var commission: Long,

    @SerializedName("usedAcquirer") var usedAcquirer: Boolean,
    @SerializedName("atk") var atk: String,

    @SerializedName("order") var order: String,
)

data class SyncPosPassPostData(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") var createdAt: String,
    @SerializedName("printingRetries") var printingRetries: Int,
    @SerializedName("type") var type: String,
)

data class SyncPosAcquisitionPostData(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") var createdAt: String,

    @SerializedName("name") var name: String,
    @SerializedName("logo") var logo: String,
    @SerializedName("price") var price: Long,
    @SerializedName("category") var category: String,

    @SerializedName("product") var product: String,
    @SerializedName("order") var order: String,
    @SerializedName("pass") var pass: String,

    @SerializedName("voucher") var voucher: String,
    @SerializedName("refund") var refund: String,
    @SerializedName("consumption") var consumption: String,
)

data class SyncPosRefundPostData(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") var createdAt: String,
)

data class SyncPosConsumptionPostData(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") var createdAt: String,
)

data class SyncPosVoucherPostData(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") var createdAt: String,
)

data class SyncPosVoucherRedemptionPostData(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") var createdAt: String,
    @SerializedName("amount") var amount: Long,
    @SerializedName("voucher") var voucher: String,
)

data class SyncPosCashupPostData(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") var createdAt: String,
    @SerializedName("initial") var initial: Long,
    @SerializedName("taken") var taken: Long,
    @SerializedName("remaining") var remaining: Long,
)

data class LoginPostData(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("serial") val serial: String
)

data class LoginQrcodePostData(
    @SerializedName("hash") val hash: String,
    @SerializedName("serial") val serial: String
)

data class RegisterDevicePostData(
    @SerializedName("name") val name: String,
    @SerializedName("serial") val serial: String
)

data class PatchPingPosUsecaseModel(
    @SerializedName("serial") val serial: String,
    @SerializedName("cashier") val cashier: String?,
    @SerializedName("eventId") val eventId: Int?,
    @SerializedName("posId") val posId: Int?,
    @SerializedName("coords") val coords: String
)

data class TESTCashierModel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class TESTEventModel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("logo") val logo: String,
    @SerializedName("pin") val pin: String,
    @SerializedName("details") val details: String,
    @SerializedName("dateStart") val dateStart: String,
    @SerializedName("dateEnd") val dateEnd: String,
    @SerializedName("printingPriceEnabled") val printingPriceEnabled: Boolean,
    @SerializedName("ticketsPrintingGrouped") val ticketsPrintingGrouped: Boolean,
    @SerializedName("isSelected") val isSelected: Boolean,
    @SerializedName("mode") val mode: String,
    @SerializedName("hasProducts") val hasProducts: Boolean,
    @SerializedName("isCreditEnabled") val isCreditEnabled: Boolean,
    @SerializedName("isDebitEnabled") val isDebitEnabled: Boolean,
    @SerializedName("isPIXEnabled") val isPIXEnabled: Boolean,
    @SerializedName("isVREnabled") val isVREnabled: Boolean,
    @SerializedName("isLnBTCEnabled") val isLnBTCEnabled: Boolean,
    @SerializedName("isCashEnabled") val isCashEnabled: Boolean,
    @SerializedName("isAcquirerPaymentEnabled") val isAcquirerPaymentEnabled: Boolean,
    @SerializedName("isMultiPaymentEnabled") val isMultiPaymentEnabled: Boolean
)

data class TESTPosModel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("cashier") val cashier: String,
    @SerializedName("isClosed") val isClosed: Boolean,
    @SerializedName("isSelected") val isSelected: Boolean,
    @SerializedName("commission") val commission: Long
)

data class TESTProductModel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("url") val url: String,
    @SerializedName("category") val category: String,
    @SerializedName("price") val price: Long,
    @SerializedName("isEnabled") val isEnabled: Boolean
)

data class TESTCategoryModel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class TESTOrderModel(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("coords") val coords: String,
    @SerializedName("synced") val synced: Boolean
)

data class TESTPaymentModel(
    @SerializedName("id") val id: String,
    @SerializedName("acquirerTransactionKey") val acquirerTransactionKey: String,
    @SerializedName("amount") val amount: Long,
    @SerializedName("commission") val commission: Long,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("order") val order: String,
    @SerializedName("type") val type: String,
    @SerializedName("usedAcquirer") val usedAcquirer: Boolean,
    @SerializedName("synced") val synced: Boolean
)

data class TESTAcquisitionModel(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("name") val name: String,
    @SerializedName("logo") val logo: String,
    @SerializedName("price") val price: Long,
    @SerializedName("commission") val commission: Long,
    @SerializedName("category") val category: String,
    @SerializedName("product") val product: String,
    @SerializedName("order") val order: String,
    @SerializedName("pass") val pass: String,
    @SerializedName("event") val event: String,
    @SerializedName("pos") val pos: String,
    @SerializedName("synced") val synced: Boolean,
    @SerializedName("voucher") val voucher: String?,
    @SerializedName("refund") val refund: String?,
    @SerializedName("consumption") val consumption: String?
)

data class TESTPassModel(
    @SerializedName("id") val id: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("accountable") val accountable: String,
    @SerializedName("printingRetries") val printingRetries: Boolean,
    @SerializedName("order") val order: String,
    @SerializedName("event") val event: String,
    @SerializedName("pos") val pos: String,
    @SerializedName("isGrouped") val isGrouped: Boolean,
    @SerializedName("synced") val synced: Boolean
)

data class TestData(
    @SerializedName("cashiers") val cashiers: List<TESTCashierModel>,
    @SerializedName("events") val events: List<TESTEventModel>,
    @SerializedName("pos") val pos: List<TESTPosModel>,
    @SerializedName("products") val products: List<TESTProductModel>,
    @SerializedName("categories") val categories: List<TESTCategoryModel>,
    @SerializedName("orders") val orders: List<TESTOrderModel>,
    @SerializedName("payments") val payments: List<TESTPaymentModel>,
    @SerializedName("acquisitions") val acquisitions: List<TESTAcquisitionModel>,
    @SerializedName("passes") val passes: List<TESTPassModel>
)

/**
 * Used to connect to the Ticpass API
 */
interface APIService {

    @GET("/events/{eventId}/product/thumbnail/download/test")
    suspend fun test(
        @Path("eventId") eventId: String,
    ): TestData

    @POST("users/token/app")
    suspend fun login(
        @Body postData: LoginPostData,
    ): APITestResponse

    @POST("events/{eventId}/pos/{posId}/sync")
    suspend fun syncPos(
        @Path("eventId") eventId: String,
        @Path("posId") posId: String,
        @Header("x-access-token") authorization: String,
        @Body postData: SyncPosPostData,
    ): SyncPosResponse

    @POST("/users/token/qrcode")
    suspend fun loginQrcode(
        @Body postData: LoginQrcodePostData,
    ): APITestResponse

    @POST("devices")
    suspend fun registerDevice(
        @Body postData: RegisterDevicePostData,
    ): APITestResponse

    @GET("events")
    suspend fun getEvents(
        @Query("fkUser") user: String,
        @Header("x-access-token") authorization: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): GetEventsResponse

    @GET("events/membership")
    suspend fun getMembership(
        @Header("x-access-token") authorization: String,
    ): GetMembershipResponse

    @GET("cashiers")
    suspend fun getPosList(
        @Query("fkEvent") event: String,
        @Header("x-access-token") authorization: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): GetPosListResponse

    @PATCH("/cashiers/closing")
    suspend fun closePos(
        @Query("idCashier") posId: String,
        @Header("x-access-token") authorization: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): PatchPosResponse

    @PATCH("/cashiers/opening")
    suspend fun openPos(
        @Query("idCashier") posId: String,
        @Query("cashierName") cashier: String,
        @Query("serial") serial: String,
        @Header("x-access-token") authorization: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): PatchPosResponse

    @PATCH("/devices/ping")
    suspend fun pingDevice(
        @Body pingData: PatchPingPosUsecaseModel
    ): PatchPingDeviceResponse

    @GET("events/{eventId}/products")
    suspend fun getEventProducts(
        @Header("x-access-token") authorization: String,
        @Path("eventId") event: String,
        @Query("limit") limit: String = "disable",
        @Query("groupBy") groupBy: String = "category",
        @Query("page") page: Int = 1,
    ): GetEventProductsResponse

    @GET("events/{menuId}/product/thumbnail/download/all")
    suspend fun downloadAllProductThumbnails(
        @Path("menuId") menuId: String,
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    companion object {
        private var BASE_URL = "$API_HOST/"

        fun create(): APIService {
            val logger = HttpLoggingInterceptor().apply { level = Level.BASIC }

            val client = OkHttpClient.Builder()
                .connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(logger)
                .build()

            Log.d("req:params", client.toString())
            Log.d("req:params", client.toString())

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(APIService::class.java)
        }
    }
}