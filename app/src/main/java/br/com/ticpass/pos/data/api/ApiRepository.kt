package br.com.ticpass.pos.data.api

import android.os.Build
import android.util.Log
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity
import br.com.ticpass.pos.data.room.entity.CashupEntity
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.data.room.entity.ConsumptionEntity
import br.com.ticpass.pos.data.room.entity.OrderEntity
import br.com.ticpass.pos.data.room.entity.PassEntity
import br.com.ticpass.pos.data.room.entity.PaymentEntity
import br.com.ticpass.pos.data.room.entity.RefundEntity
import br.com.ticpass.pos.data.room.entity.VoucherEntity
import br.com.ticpass.pos.data.room.entity.VoucherRedemptionEntity
import br.com.ticpass.pos.data.api.LoginQrcodePostData
import javax.inject.Inject
import retrofit2.Response
import okhttp3.ResponseBody



class APIRepository @Inject constructor(
    private val service: APIService,
    private val categoryRepository: CategoryRepository,
) {

    suspend fun syncPos(
        eventId: String,
        posId: String,
        orders: List<OrderEntity>,
        payments: List<PaymentEntity>,
        cashups: List<CashupEntity>,
        vouchers: List<VoucherEntity>,
        voucherRedemptions: List<VoucherRedemptionEntity>,
        refunds: List<RefundEntity>,
        acquisitions: List<AcquisitionEntity>,
        consumptions: List<ConsumptionEntity>,
        passes: List<PassEntity>,
        jwt: String,
    ): SyncPosResponse {

        var response = SyncPosResponse(
            status = 0,
            message = "0",
            result = emptyList(),
            error = "",
            name = "",
        )

        try {
            response = service.syncPos(
                eventId,
                posId,
                authorization = "Bearer $jwt",
                SyncPosPostData(
                    orders = orders.map {
                        SyncPosOrderPostData(
                            id = it.id,
                            coords = it.coords,
                            createdAt = it.createdAt,
                        )
                    },
                    payments = payments.map {
                        SyncPosPaymentPostData(
                            id = it.id,
                            type = it.type,
                            createdAt = it.createdAt,
                            amount = it.amount,
                            commission = it.commission,
                            usedAcquirer = it.usedAcquirer,
                            atk = it.acquirerTransactionKey,
                            order = it.order,
                        )
                    },
                    passes = passes.map {
                        SyncPosPassPostData(
                            id = it.id,
                            createdAt = it.createdAt,
                            printingRetries = it.printingRetries,
                            type = if(it.isGrouped) "group" else "default",
                        )
                    },
                    acquisitions = acquisitions.map {
                        SyncPosAcquisitionPostData(
                            id = it.id,
                            createdAt = it.createdAt,
                            name = it.name,
                            logo = it.logo,
                            price = it.price,
                            category = it.category,
                            product = it.product,
                            order = it.order,
                            pass = it.pass,
                            voucher = it.voucher,
                            refund = it.refund,
                            consumption = it.consumption,
                        )
                    },
                    refunds = refunds.map {
                        SyncPosRefundPostData(
                            id = it.id,
                            createdAt = it.createdAt,
                        )
                    },
                    consumptions = consumptions.map {
                        SyncPosConsumptionPostData(
                            id = it.id,
                            createdAt = it.createdAt,
                        )
                    },
                    vouchers = vouchers.map {
                        SyncPosVoucherPostData(
                            id = it.id,
                            createdAt = it.createdAt,
                        )
                    },
                    voucherRedemptions = voucherRedemptions.map {
                        SyncPosVoucherRedemptionPostData(
                            id = it.id,
                            createdAt = it.createdAt,
                            amount = it.amount,
                            voucher = it.voucher,
                        )
                    },
                    cashups = cashups.map {
                        SyncPosCashupPostData(
                            id = it.id,
                            createdAt = it.createdAt,
                            initial = it.initial,
                            taken = it.taken,
                            remaining = it.remaining,
                        )
                    },
                ),
            )
        } catch (e: Exception) {
            Log.d("syncPos:error", "$e")
        }

        return response
    }

    suspend fun login(email: String, password: String, serial: String): APITestResponse {
        val response = service.login(
            LoginPostData(
                email,
                password,
                serial,
            )
        )

        return response
    }

    suspend fun test(): TestData {
        val response = service.test("4")

        return response
    }

    suspend fun loginQrcode(hash: String, serial: String): APITestResponse {
        val response = service.loginQrcode(
            LoginQrcodePostData(
                hash,
                serial,
            )
        )

        return response
    }

    suspend fun registerDevice(
        name: String,
        serial: String
    ): APITestResponse {
        val response = service.registerDevice(
            RegisterDevicePostData(
                name,
                serial,
            )
        )

        return response
    }

    suspend fun getMembership(
        jwt: String,
    ): GetMembershipResponse {
        var response = GetMembershipResponse(
            status = 401,
            message = "unable to reach server",
            result = GetMembershipResult(
                expiration = "2099-01-01 23:59:59.000 -0400"
            )
        )

        try {
            response = service.getMembership(
                authorization = "Bearer $jwt",
            )
        } catch (e: Exception) {
            Log.d("api:exception", e.toString())
        }

        return response
    }

    suspend fun getEvents(
        user: String,
        jwt: String,
        page: Int = 1,
        limit: Int = 100,
    ): GetEventsResponse {
        var response = GetEventsResponse(
            status = 401,
            message = "unable to reach server",
            result = GetEventsResult(
                items = emptyList()
            )
        )

        try {
            response = service.getEvents(
                user,
                authorization = "Bearer $jwt",
                page,
                limit,
            )
        } catch (e: Exception) {
            Log.d("api:exception", e.toString())
        }

        return response
    }

    suspend fun getPosList(
        event: String,
        jwt: String,
        page: Int = 1,
        limit: Int = 100,
    ): GetPosListResponse {
        var response = GetPosListResponse(
            status = 401,
            message = "unable to reach server",
            result = GetPosListResult(
                items = emptyList()
            )
        )

        try {
            response = service.getPosList(
                event,
                authorization = "Bearer $jwt",
                page,
                limit,
            )
        } catch (e: Exception) {
            Log.d("api:exception", e.toString())
        }

        return response
    }

    suspend fun getEventProducts(
        event: String,
        jwt: String,
    ): GetEventProductsResponse {
        var response = GetEventProductsResponse(
            status = 401,
            message = "unable to reach server",
            result = emptyList(),
            error = "unable to reach server",
            name = "unable to reach server",
        )

        try {
            response = service.getEventProducts(
                event = event,
                authorization = "Bearer $jwt",
            )
        } catch (e: Exception) {
            Log.d("api:exception", e.toString())
        }

        return response
    }

    suspend fun pingDevice(
        serial: String,
        coords: String,
        posId: Int? = null,
        eventId: Int? = null,
        cashier: String? = null,
    ): PatchPingDeviceResponse {
        var response = PatchPingDeviceResponse(
            status = 401,
            message = "unable to reach server"
        )

        val data = PatchPingPosUsecaseModel(
            eventId = eventId,
            posId = posId,
            serial = serial,
            coords = coords,
            cashier = cashier,
        )

        try {
            response = service.pingDevice(
                data,
            )
        } catch (e: Exception) {
            Log.d("api:exception", e.toString())
        }

        return response
    }

    suspend fun openPos(
        posId: String,
        cashierName: String,
        jwt: String,
    ): PatchPosResponse {
        var response = PatchPosResponse(
            status = 401,
            message = "unable to reach server"
        )

        try {
            response = service.openPos(
                posId = posId,
                cashier = cashierName,
                serial = Build.SERIAL,
                authorization = "Bearer $jwt",
            )
        } catch (e: Exception) {
            Log.d("api:exception", e.toString())
        }

        return response
    }

    suspend fun closePos(
        posId: String,
        jwt: String,
    ): PatchPosResponse {
        var response = PatchPosResponse(
            status = 401,
            message = "unable to reach server"
        )

        try {
            response = service.closePos(
                posId = posId,
                authorization = "Bearer $jwt",
            )
        } catch (e: Exception) {
            Log.d("api:exception", e.toString())
        }

        return response
    }

    suspend fun downloadAllProductThumbnails(
        menuId: String,
        jwt: String
    ): Response<ResponseBody> {
        return try {
            val response = service.downloadAllProductThumbnails(
                menuId = menuId,
                authorization = "Bearer $jwt"
            )

            if (!response.isSuccessful) {
                Log.e("APIRepository", "Error downloading thumbnails: ${response.code()}")
            }

            response
        } catch (e: Exception) {
            Log.e("APIRepository", "Error downloading thumbnails", e)
            Response.error(500, ResponseBody.create(null, e.message ?: "Unknown error"))
        }
    }

    suspend fun getRefreshToken(refreshToken: String): RefreshTokenResponse {
        return try {
            service.refreshToken(
                RefreshTokenRequest(refresh = refreshToken)
            )
        } catch (e: Exception) {
            Log.e("APIRepository", "Erro ao fazer refresh do token", e)
            throw e
        }
    }



    companion object {
        private const val NETWORK_PAGE_SIZE = 25
    }
}
