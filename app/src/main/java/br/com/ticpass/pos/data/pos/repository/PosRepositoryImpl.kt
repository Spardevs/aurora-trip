package br.com.ticpass.pos.data.pos.repository

import br.com.ticpass.pos.data.pos.datasource.PosLocalDataSource
import br.com.ticpass.pos.data.pos.datasource.PosRemoteDataSource
import br.com.ticpass.pos.data.pos.mapper.toDomain
import br.com.ticpass.pos.data.pos.mapper.toEntity
import br.com.ticpass.pos.domain.pos.model.Pos
import br.com.ticpass.pos.domain.pos.repository.PosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PosRepositoryImpl(
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
            val response = remoteDataSource.getPosList(
                take, page, menu, available, authorization, cookie
            )

            if (response.isSuccessful && response.body() != null) {
                val posDtos = response.body()!!.edges
                val posEntities = posDtos.map { it.toEntity() }

                // Salvar no banco de dados
                localDataSource.savePosList(posEntities)

                // Retornar os dados mapeados para o domínio
                Result.success(posDtos.map { it.toDomain() })
            } else {
                Result.failure(Exception("Failed to fetch POS data: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}