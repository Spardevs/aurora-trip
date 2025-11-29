package br.com.ticpass.pos.presentation.login.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val refreshPosListUseCase: RefreshPosListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginPosUiState>(LoginPosUiState.Loading)
    val uiState: StateFlow<LoginPosUiState> = _uiState

    fun observeMenu(menuId: String, authorization: String, cookie: String) {
        viewModelScope.launch {
            getPosByMenuUseCase(menuId)
                .catch { _uiState.value = LoginPosUiState.Error(it.message ?: "Erro") }
                .collectLatest { cached ->
                    if (cached.isEmpty()) _uiState.value = LoginPosUiState.Empty
                    else _uiState.value = LoginPosUiState.Success(cached)

                    // try refresh
                    val res = refreshPosListUseCase(10, 1, menuId, "both", authorization, cookie)
                    res.onSuccess { refreshed ->
                        _uiState.value = LoginPosUiState.Success(refreshed)
                    }.onFailure { ex ->
                        if (cached.isEmpty()) _uiState.value = LoginPosUiState.Error(ex.message ?: "Falha ao atualizar")
                    }
                }
        }
    }
}