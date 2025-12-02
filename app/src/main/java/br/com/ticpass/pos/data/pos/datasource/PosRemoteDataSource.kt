package br.com.ticpass.pos.data.pos.datasource

import br.com.ticpass.pos.data.pos.remote.dto.PosResponseDto
import br.com.ticpass.pos.data.pos.remote.dto.SessionDto
import br.com.ticpass.pos.data.pos.remote.service.OpenSessionRequest
import br.com.ticpass.pos.data.pos.remote.service.PosApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosRemoteDataSource @Inject constructor(
    private val api: PosApiService
) {
    suspend fun getMenuPos(
        take: Int,
        page: Int,
        menu: String,
        available: String,
    ): Response<PosResponseDto> {
        return api.getPosList(take, page, menu, available)
    }

    suspend fun openPosSession(posId: String, deviceId: String, cashierId: String): Response<SessionDto> {
        val request = OpenSessionRequest(
            pos = posId,
            device = deviceId,
            cashier = cashierId
        )
        return api.openPosSession(request)
    }
}