package br.com.ticpass.pos.presentation.login.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.domain.pos.repository.PosRepository
import br.com.ticpass.pos.domain.product.repository.ProductRepository
import br.com.ticpass.pos.domain.product.usecase.RefreshProductsUseCase
import br.com.ticpass.pos.domain.user.repository.UserRepository
import br.com.ticpass.pos.domain.category.usecase.RefreshCategoriesUseCase
import br.com.ticpass.pos.domain.menupin.usecase.RefreshMenuPinsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LoadingDownloadViewModel @Inject constructor(
    private val refreshCategoriesUseCase: RefreshCategoriesUseCase,
    private val refreshProductsUseCase: RefreshProductsUseCase,
    private val refreshMenuPinsUseCase: RefreshMenuPinsUseCase,
    private val productRepository: ProductRepository,
    private val posRepository: PosRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadingDownloadUiState>(LoadingDownloadUiState.Idle)
    val uiState: StateFlow<LoadingDownloadUiState> = _uiState

    fun startCompleteProcess(menuId: String, posId: String, deviceId: String, cashierName: String, userId: String) {
        viewModelScope.launch {
            try {
                // Etapa 1: Download de categorias
                _uiState.value = LoadingDownloadUiState.Loading("Baixando categorias...")
                refreshCategoriesUseCase(menuId)

                // Etapa 2: Download de produtos
                _uiState.value = LoadingDownloadUiState.Loading("Baixando produtos...")
                refreshProductsUseCase(menuId)

                // Etapa 3: Download de thumbnails
                _uiState.value = LoadingDownloadUiState.Loading("Baixando thumbnails...")
                val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
                productRepository.downloadAndExtractThumbnails(menuId, thumbnailsDir)

                // Etapa 4: Download de PINs do menu (whitelist)
                _uiState.value = LoadingDownloadUiState.Loading("Baixando PINs autorizados...")
                refreshMenuPinsUseCase(menuId)

                // Etapa 5: Login do usuário
                _uiState.value = LoadingDownloadUiState.Loading("Logando usuário...")
                userRepository.setUserLogged(userId, true)

                // Etapa 6: Configuração do POS
                _uiState.value = LoadingDownloadUiState.Loading("Configurando POS...")
                posRepository.selectPos(posId).onFailure { exception ->
                    throw exception
                }

                // Etapa 7: Abertura da sessão do POS
                _uiState.value = LoadingDownloadUiState.Loading("Abrindo POS...")
                posRepository.openPosSession(posId, deviceId, cashierName).onFailure { exception ->
                    throw exception
                }

                // Todas as etapas concluídas com sucesso
                _uiState.value = LoadingDownloadUiState.Success("Processo completo!")
            } catch (e: Exception) {
                Timber.tag("LoadingDownload").e(e, "Erro no processo: ${e.message}")
                _uiState.value = LoadingDownloadUiState.Error("Erro no processo: ${e.message}")
            }
        }
    }
}

sealed class LoadingDownloadUiState {
    object Idle : LoadingDownloadUiState()
    data class Loading(val message: String) : LoadingDownloadUiState()
    data class Success(val message: String) : LoadingDownloadUiState()
    data class Error(val message: String) : LoadingDownloadUiState()
}