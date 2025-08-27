package br.com.ticpass.pos.data.api

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

data class APITestResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: Result,
    @SerializedName("error") val error: String?,
    @SerializedName("name") val name: String?
)

data class Result(
    @SerializedName("token") val token: String,
    @SerializedName("tokenRefresh") val tokenRefresh: String,
    @SerializedName("user") val user: User
)

data class SyncPosResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: List<Any>,
    @SerializedName("error") val error: String?,
    @SerializedName("name") val name: String?
)

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("hash") val hash: String,
    @SerializedName("qrcode") val qrcode: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("deletedAt") val deletedAt: String?,
    @SerializedName("operator") val operator: Any?,
    @SerializedName("blockedAt")       val blockedAt: String?,
    @SerializedName("membershipExpAt") val membershipExpAt: String?,
    @SerializedName("permission")      val permission: Permission

)

data class GetEventsResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: GetEventsResult
)

data class GetMembershipResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: GetMembershipResult
)

data class GetMembershipResult(
    @SerializedName("expiration") val expiration: String,
)

data class GetEventsResult(
    @SerializedName("items") val items: List<EventItem>
)

data class EventItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("dateStart") val dateStart: String,
    @SerializedName("dateEnd") val dateEnd: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("pin") val pin: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("deletedAt") val deletedAt: String?,
    @SerializedName("status") val status: String,
    @SerializedName("ticket") val ticket: String,
    @SerializedName("isInvoice") val isInvoice: Boolean,
    @SerializedName("isPrintTicket") val isPrintTicket: Boolean,
    @SerializedName("isQrcodeValidation") val isQRCodeValidation: Boolean,
    @SerializedName("numberParticipants") val numberParticipants: Long,
    @SerializedName("goal") val goal: Int,
    @SerializedName("details") val details: String,
    @SerializedName("categories") val categories: List<String>,
    @SerializedName("products") val products: List<String>,
    @SerializedName("type") val type: String,
    @SerializedName("mode") val mode: String,
    @SerializedName("isCreditEnabled") val isCreditEnabled: Boolean,
    @SerializedName("isDebitEnabled") val isDebitEnabled: Boolean,
    @SerializedName("isPIXEnabled") val isPIXEnabled: Boolean,
    @SerializedName("isVREnabled") val isVREnabled: Boolean,
    @SerializedName("isLnBTCEnabled") val isLnBTCEnabled: Boolean,
    @SerializedName("isCashEnabled") val isCashEnabled: Boolean,
    @SerializedName("isAcquirerPaymentEnabled") val isAcquirerPaymentEnabled: Boolean,
    @SerializedName("isMultiPaymentEnabled") val isMultiPaymentEnabled: Boolean,
)

data class PosItemSession(
    @SerializedName("id") val id: String,
    @SerializedName("closing") val closing: String?,
    @SerializedName("cashier") val cashier: String
)

data class PosItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("deleted_at") val deletedAt: String?,
    @SerializedName("type") val type: String,
    @SerializedName("prefix") val prefix: String?,
    @SerializedName("sequence") val sequence: String?,
    @SerializedName("commission") val commission: BigInteger?,
    @SerializedName("session") val session: PosItemSession?
)

data class GetPosListResult(@SerializedName("items") val items: List<PosItem>)

data class GetPosListResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: GetPosListResult
)

data class GetEventProductsResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: List<Category>,
    @SerializedName("error") val error: String?,
    @SerializedName("name") val name: String?
)

data class Category(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("products") val products: List<Product>
)

@Parcelize
data class Product(
    @SerializedName("id") val id: String,
    @SerializedName("photo") val photo: String,
    @SerializedName("title") val title: String,
    @SerializedName("value") val value: BigInteger,
    @SerializedName("stock") val stock: BigInteger,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("deleted_at") val deletedAt: String?,
    @SerializedName("fk_category") val fkCategory: String,
    @SerializedName("fk_event") val fkEvent: Int
) : Parcelable

data class PatchPosResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
)

data class PatchPingDeviceResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
)

data class RefreshTokenRequest(
    @SerializedName("refresh") val refresh: String
)

data class Permission(
    @SerializedName("id")          val id: Int,
    @SerializedName("name")        val name: String,
    @SerializedName("number")      val number: Int,
    @SerializedName("createdAt")   val createdAt: String,
    @SerializedName("updatedAt")   val updatedAt: String,
    @SerializedName("deletedAt")   val deletedAt: String?
)

data class RefreshTokenResult(
    @SerializedName("token")        val token: String,
    @SerializedName("tokenRefresh") val tokenRefresh: String,
    @SerializedName("user")         val user: User
)

data class RefreshTokenResponse(
    @SerializedName("status")  val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result")  val result: RefreshTokenResult
)