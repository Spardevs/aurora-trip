package br.com.ticpass.pos.data.menu.repository

import android.content.Context
import br.com.ticpass.pos.data.menu.datasource.MenuLocalDataSource
import br.com.ticpass.pos.data.menu.datasource.MenuRemoteDataSource
import br.com.ticpass.pos.data.menu.mapper.toDomainModel
import br.com.ticpass.pos.data.menu.mapper.toEntity
import br.com.ticpass.pos.domain.menu.repository.MenuRepository
import br.com.ticpass.pos.domain.menu.model.MenuDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class MenuRepositoryImpl @Inject constructor(
    private val remoteDataSource: MenuRemoteDataSource,
    private val logoRemoteDataSource: MenuRemoteDataSource,
    private val localDataSource: MenuLocalDataSource,
    private val context: Context
) : MenuRepository {

    override fun getMenuItems(take: Int, page: Int): Flow<List<MenuDb>> = flow {
        try {
            // 1) Tenta emitir cache primeiro
            val cachedEntities = localDataSource.getAllMenus().first()
            if (cachedEntities.isNotEmpty()) {
                val cachedDomain: List<MenuDb> = cachedEntities.map { it.toDomainModel() }
                emit(cachedDomain)
            }

            Timber.d("MenuRepositoryImpl.getMenuItems - tentando obter cache e remote")
            val response = remoteDataSource.getMenu(take, page)
            Timber.d("MenuRepositoryImpl.getMenuItems - remote response edges=${response.edges.size}")
            val remoteDomain = response.edges.map { it.toDomainModel() } // List<Menu>
            val entities = remoteDomain.map { it.toEntity() }             // List<MenuEntity>
            localDataSource.insertMenus(entities)

            // 2) Dispara downloads de logo em paralelo, sem travar o fluxo
            try {
                coroutineScope {
                    entities.forEach { entity ->
                        val logoId = entity.logo

                        // Regra: se não tiver logo, não faz requisição
                        if (logoId.isNullOrBlank()) {
                            Timber.d(
                                "MenuRepositoryImpl.getMenuItems - menu ${entity.id} sem logo, ignorando download"
                            )
                            return@forEach
                        }

                        launch(Dispatchers.IO) {
                            try {
                                val body = logoRemoteDataSource.downloadLogo(logoId)
                                val file = writeResponseBodyToDisk(body, logoId)
                                file?.let {
                                    val updated = entity.copy(logo = it.absolutePath)
                                    localDataSource.insertOrUpdateMenu(updated)
                                }
                            } catch (e: Exception) {
                                // Regra: se der erro ao baixar logo, só loga; não pode travar fluxo
                                Timber.e(e, "Erro ao baixar logo id=$logoId")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Qualquer problema ao iniciar/paralelizar downloads NÃO deve quebrar o getMenuItems
                Timber.w(e, "Falha ao iniciar downloads de logo (ignorando para não travar fluxo)")
            }

            // 3) Emite a lista remota independente do resultado dos logos
            val remoteAsDb: List<MenuDb> = entities.map { it.toDomainModel() }
            emit(remoteAsDb)
        } catch (e: Exception) {
            // Em caso de falha no remoto de menu, tenta cair para o cache
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
        // Regra: se não tiver logoId válido, não faz requisição
        if (logoId.isBlank()) {
            Timber.d("MenuRepositoryImpl.downloadLogo - logoId em branco, não chamando API")
            emit(null)
            return@flow
        }

        Timber.d("MenuRepositoryImpl.downloadLogo called logoId=$logoId")
        try {
            val responseBody: ResponseBody = logoRemoteDataSource.downloadLogo(logoId)
            Timber.d("MenuRepositoryImpl.downloadLogo got responseBody for $logoId")
            val file = writeResponseBodyToDisk(responseBody, logoId)
            Timber.d("MenuRepositoryImpl.downloadLogo wrote file=$file for $logoId")
            emit(file)
        } catch (e: Exception) {
            // Se der erro ao buscar o logo, não faz nada além de logar e emitir null
            Timber.e(e, "MenuRepositoryImpl.downloadLogo failed for $logoId")
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
            Timber.e(e, "Erro ao salvar logo em disco logoId=$logoId")
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

    override suspend fun updateMenuLogoPath(menuId: String, localPath: String) {
        try {
            val existing = localDataSource.getMenuById(menuId)
            if (existing != null) {
                val updated = existing.copy(logo = localPath)
                localDataSource.insertOrUpdateMenu(updated)
            } else {
                Timber.w("updateMenuLogoPath: menuId=$menuId not found in local DB")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update menu logo path for $menuId")
        }
    }
}