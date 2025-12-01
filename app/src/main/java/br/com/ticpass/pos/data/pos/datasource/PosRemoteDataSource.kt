package br.com.ticpass.pos.data.pos.datasource

import br.com.ticpass.pos.data.pos.remote.dto.PosResponseDto
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
        authorization: String,
        cookie: String
    ): Response<PosResponseDto> {
        return api.getPosList(take, page, menu, available, authorization, cookie)
    }
}