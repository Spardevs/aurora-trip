package br.com.ticpass.pos.domain.menu.usecase

import br.com.ticpass.pos.domain.menu.repository.MenuRepository
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class DownloadMenuLogoUseCase @Inject constructor(
    private val menuLogoRepository: MenuRepository
) {
    operator fun invoke(logoId: String): Flow<File?> {
        return menuLogoRepository.downloadLogo(logoId)
    }
}

class GetMenuLogoFileUseCase @Inject constructor(
    private val menuLogoRepository: MenuRepository
) {
    operator fun invoke(logoId: String): File? {
        return menuLogoRepository.getLogoFile(logoId)
    }
}

class GetAllMenuLogoFilesUseCase @Inject constructor(
    private val menuLogoRepository: MenuRepository
) {
    operator fun invoke(): List<File> {
        return menuLogoRepository.getAllLogoFiles()
    }
}