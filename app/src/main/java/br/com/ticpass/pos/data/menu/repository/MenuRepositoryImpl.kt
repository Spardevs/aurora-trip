package br.com.ticpass.pos.data.menu.repository

import android.content.Context
import br.com.ticpass.pos.data.menu.datasource.MenuLocalDataSource
import br.com.ticpass.pos.data.menu.datasource.MenuRemoteDataSource
import br.com.ticpass.pos.data.menu.mapper.toDomainModel
import br.com.ticpass.pos.data.menu.mapper.toEntity
import br.com.ticpass.pos.domain.menu.repository.MenuRepository
import br.com.ticpass.pos.domain.menu.model.Menu
import br.com.ticpass.pos.domain.menu.model.MenuDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject

class MenuRepositoryImpl @Inject constructor(
    private val remoteDataSource: MenuRemoteDataSource,
    private val logoRemoteDataSource: MenuRemoteDataSource,
    private val localDataSource: MenuLocalDataSource,
    private val context: Context
) : MenuRepository {

    // assinatura
    // file: MenuRepositoryImpl.kt (inside class)
    override fun getMenuItems(take: Int, page: Int): Flow<List<MenuDb>> = flow {
        try {
            // cache (MenuEntity -> MenuDb)
            val cachedEntities = localDataSource.getAllMenus().first()
            if (!cachedEntities.isNullOrEmpty()) {
                val cachedDomain: List<MenuDb> = cachedEntities.map { it.toDomainModel() } // MenuEntity -> MenuDb
                emit(cachedDomain)
            }

            // remote: fetch remote Menu (domain Menu), convert to entities and persist
            val response = remoteDataSource.getMenu(take, page)
            val remoteDomain = response.edges.map { it.toDomainModel() } // List<Menu> (remote -> domain Menu)
            val entities = remoteDomain.map { it.toEntity() } // Menu -> MenuEntity
            localDataSource.insertMenus(entities)

            // convert persisted entities -> MenuDb and emit
            val remoteAsDb: List<MenuDb> = entities.map { it.toDomainModel() } // MenuEntity -> MenuDb
            emit(remoteAsDb)
        } catch (e: Exception) {
            // fallback to cached
            try {
                val cached = localDataSource.getAllMenus().first()
                val domain: List<MenuDb> = cached.map { it.toDomainModel() }
                emit(domain)
            } catch (_: Exception) {
                emit(emptyList())
            }
        }
    }

    private val logoDirectory: File by lazy {
        File(context.filesDir, "MenuLogos").apply {
            if (!exists()) mkdirs()
        }
    }

    override fun downloadLogo(logoId: String): Flow<File?> = flow {
        try {
            val responseBody: ResponseBody = logoRemoteDataSource.downloadLogo(logoId)
            val file = writeResponseBodyToDisk(responseBody, logoId)
            emit(file)
        } catch (e: Exception) {
            emit(null)
        }
    }

    private fun writeResponseBodyToDisk(body: ResponseBody, logoId: String): File? {
        return try {
            val file = File(logoDirectory, "logo_$logoId.png")
            file.outputStream().use { outputStream ->
                body.byteStream().copyTo(outputStream)
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    override fun getLogoFile(logoId: String): File? {
        val file = File(logoDirectory, "logo_$logoId.png")
        return if (file.exists()) file else null
    }

    override fun getAllLogoFiles(): List<File> {
        return logoDirectory.listFiles()?.toList() ?: emptyList()
    }
}