package br.com.ticpass.pos.presentation.login.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.domain.pos.repository.PosRepository
import br.com.ticpass.pos.domain.pos.usecase.GetPosByMenuUseCase
import br.com.ticpass.pos.domain.pos.usecase.RefreshPosListUseCase
import br.com.ticpass.pos.presentation.login.states.LoginPosUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginPosViewModel @Inject constructor(
    private val getPosByMenuUseCase: GetPosByMenuUseCase,
    private val refreshPosListUseCase: RefreshPosListUseCase,
    private val posRepository: PosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginPosUiState>(LoginPosUiState.Loading)
    val uiState: StateFlow<LoginPosUiState> = _uiState

    private var currentPage = 1
    private var hasNextPage = true
    private var isLoading = false

    fun loadPosList(menuId: String, authorization: String, cookie: String, page: Int = 1) {
        if (isLoading) return

        isLoading = true
        viewModelScope.launch {
            if (page == 1) {
                _uiState.value = LoginPosUiState.Loading
            }

            val result = refreshPosListUseCase(10, page, menuId, "both")
            result.onSuccess { posList ->
                hasNextPage = false // Adjust as needed

                if (page == 1) {
                    _uiState.value = LoginPosUiState.Success(posList)
                } else {
                    val currentList = (_uiState.value as? LoginPosUiState.Success)?.posList ?: emptyList()
                    _uiState.value = LoginPosUiState.Success(currentList + posList)
                }

                isLoading = false
            }.onFailure { ex ->
                isLoading = false
                if (page == 1) {
                    getPosByMenuUseCase(menuId)
                        .catch { _uiState.value = LoginPosUiState.Error(ex.message ?: "Falha ao carregar") }
                        .collectLatest { cached ->
                            if (cached.isEmpty()) {
                                _uiState.value = LoginPosUiState.Error(ex.message ?: "Falha ao carregar")
                            } else {
                                _uiState.value = LoginPosUiState.Success(cached)
                            }
                        }
                }
            }
        }
    }

    fun loadNextPage(menuId: String, authorization: String, cookie: String) {
        if (hasNextPage && !isLoading) {
            loadPosList(menuId, authorization, cookie, currentPage + 1)
        }
    }

    suspend fun closePosSession(posId: String) {
        val result = posRepository.closePosSession(posId)
        // Handle result as needed
    }

    fun refresh(menuId: String, authorization: String, cookie: String) {
        currentPage = 1
        hasNextPage = true
        loadPosList(menuId, authorization, cookie, 1)
    }

    @Deprecated("Use loadPosList instead", ReplaceWith("loadPosList(menuId, authorization, cookie, 1)"))
    fun observeMenu(menuId: String, authorization: String, cookie: String) {
        refresh(menuId, authorization, cookie)
    }
}