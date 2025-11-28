package br.com.ticpass.pos.domain.menu.repository

import br.com.ticpass.pos.domain.menu.model.MenuDb
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MenuRepository {
    fun getMenuItems(take: Int, page: Int): Flow<List<MenuDb>>
    fun downloadLogo(logoId: String): Flow<File?>
    fun getLogoFile(logoId: String): File?
    fun getAllLogoFiles(): List<File>
}