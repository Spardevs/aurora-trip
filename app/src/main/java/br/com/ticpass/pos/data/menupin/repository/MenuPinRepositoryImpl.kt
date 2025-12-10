package br.com.ticpass.pos.data.menupin.repository

import br.com.ticpass.pos.data.menupin.datasource.MenuPinLocalDataSource
import br.com.ticpass.pos.data.menupin.datasource.MenuPinRemoteDataSource
import br.com.ticpass.pos.data.menupin.mapper.toDomain
import br.com.ticpass.pos.data.menupin.mapper.toEntity
import br.com.ticpass.pos.domain.menupin.model.MenuPin
import br.com.ticpass.pos.domain.menupin.repository.MenuPinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class MenuPinRepositoryImpl @Inject constructor(
    private val localDataSource: MenuPinLocalDataSource,
    private val remoteDataSource: MenuPinRemoteDataSource
) : MenuPinRepository {
    
    override fun getPinsByMenuId(menuId: String): Flow<List<MenuPin>> {
        return localDataSource.getPinsByMenuId(menuId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getPinsByMenuIdOnce(menuId: String): List<MenuPin> {
        return localDataSource.getPinsByMenuIdOnce(menuId).map { it.toDomain() }
    }
    
    override suspend fun validatePin(menuId: String, code: String): MenuPin? {
        return localDataSource.getPinByCode(menuId, code)?.toDomain()
    }
    
    override suspend fun refreshPins(menuId: String) {
        Timber.tag("MenuPin").i("Refreshing pins for menuId: $menuId")
        try {
            val remotePins = remoteDataSource.getMenuPinSummary(menuId)
            Timber.tag("MenuPin").i("Received ${remotePins.size} pins from API")
            
            val entities = remotePins.map { it.toEntity() }
            
            // Clear existing pins for this menu and insert new ones
            localDataSource.deleteByMenuId(menuId)
            localDataSource.insertAll(entities)
            
            Timber.tag("MenuPin").i("Inserted ${entities.size} pins into local database")
        } catch (e: Exception) {
            Timber.tag("MenuPin").e(e, "Error refreshing pins: ${e.message}")
            throw e
        }
    }
    
    override suspend fun countPins(menuId: String): Int {
        return localDataSource.countByMenuId(menuId)
    }
}
