package br.com.ticpass.pos.data.menupin.datasource

import br.com.ticpass.pos.data.menupin.local.dao.MenuPinDao
import br.com.ticpass.pos.data.menupin.local.entity.MenuPinEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MenuPinLocalDataSource @Inject constructor(
    private val menuPinDao: MenuPinDao
) {
    
    suspend fun insertAll(pins: List<MenuPinEntity>) {
        menuPinDao.insertAll(pins)
    }
    
    suspend fun insert(pin: MenuPinEntity) {
        menuPinDao.insert(pin)
    }
    
    fun getPinsByMenuId(menuId: String): Flow<List<MenuPinEntity>> {
        return menuPinDao.getPinsByMenuId(menuId)
    }
    
    suspend fun getPinsByMenuIdOnce(menuId: String): List<MenuPinEntity> {
        return menuPinDao.getPinsByMenuIdOnce(menuId)
    }
    
    suspend fun getPinByCode(menuId: String, code: String): MenuPinEntity? {
        return menuPinDao.getPinByCode(menuId, code)
    }
    
    suspend fun deleteByMenuId(menuId: String) {
        menuPinDao.deleteByMenuId(menuId)
    }
    
    suspend fun deleteAll() {
        menuPinDao.deleteAll()
    }
    
    suspend fun countByMenuId(menuId: String): Int {
        return menuPinDao.countByMenuId(menuId)
    }
}
