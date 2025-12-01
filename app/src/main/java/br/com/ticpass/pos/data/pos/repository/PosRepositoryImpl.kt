package br.com.ticpass.pos.data.pos.repository

import br.com.ticpass.pos.data.pos.datasource.PosLocalDataSource
import br.com.ticpass.pos.data.pos.datasource.PosRemoteDataSource
import br.com.ticpass.pos.data.pos.mapper.toDomain
import br.com.ticpass.pos.data.pos.mapper.toEntity
import br.com.ticpass.pos.data.pos.remote.dto.PosDto
import br.com.ticpass.pos.domain.pos.model.Pos
import br.com.ticpass.pos.domain.pos.repository.PosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosRepositoryImpl @Inject constructor(
    private val remoteDataSource: PosRemoteDataSource,
    private val localDataSource: PosLocalDataSource
) : PosRepository {

    override fun getPosByMenu(menuId: String): Flow<List<Pos>> {
        return localDataSource.getPosByMenu(menuId).map { posEntities ->
            posEntities.map { it.toDomain() }
        }
    }

    override suspend fun refreshPosList(
        take: Int,
        page: Int,
        menu: String,
        available: String,
        authorization: String,
        cookie: String
    ): Result<List<Pos>> {
        return try {
            // call the remote data source method you actually defined
            val response = remoteDataSource.getMenuPos(take, page, menu, available)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to fetch POS data: ${response.message()}"))
            }

            val body = response.body() ?: return Result.failure(Exception("Empty response body"))

            val posDtos: List<PosDto> = when {
                (body::class.members.any { it.name == "edges" }) ->
                    @Suppress("UNCHECKED_CAST")
                    (body as? Any /*PosResponseDto*/)?.let { resp ->
                        resp::class.members.firstOrNull { it.name == "edges" }?.let {
                            body.edges
                        }
                    } ?: emptyList()
                false -> (body as List<PosDto>)
                else -> emptyList()
            }

            val posEntities = posDtos.map { it.toEntity() }
            localDataSource.savePosList(posEntities)

            Result.success(posDtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}