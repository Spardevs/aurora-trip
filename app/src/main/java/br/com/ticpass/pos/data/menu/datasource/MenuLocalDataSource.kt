package br.com.ticpass.pos.data.menu.datasource

import br.com.ticpass.pos.data.menu.local.dao.MenuDao
import br.com.ticpass.pos.data.menu.local.entity.MenuEntity
import javax.inject.Inject

class MenuLocalDataSource @Inject constructor(
    private val menuDao: MenuDao
) {

    fun getAllMenus() = menuDao.getAllMenus()

    suspend fun getMenuById(id: String) = menuDao.getMenuById(id)

    suspend fun insertMenus(menus: List<MenuEntity>) = menuDao.insertMenus(menus)

    suspend fun clearAllMenus() = menuDao.clearAllMenus()
}