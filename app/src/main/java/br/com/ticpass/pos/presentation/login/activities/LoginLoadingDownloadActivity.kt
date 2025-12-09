package br.com.ticpass.pos.presentation.login.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.core.util.SessionPrefsManagerUtils
import br.com.ticpass.pos.data.user.local.dao.UserDao
import br.com.ticpass.pos.presentation.login.viewmodels.LoadingDownloadUiState
import br.com.ticpass.pos.presentation.login.viewmodels.LoadingDownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.product.activities.ProductsListActivity

@AndroidEntryPoint
class LoginLoadingDownloadActivity : AppCompatActivity() {

    private val loadingDownloadViewModel: LoadingDownloadViewModel by viewModels()

    @Inject
    lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_download)
        val statusTextView: TextView = findViewById(R.id.tvDownloadProgress)

        lifecycleScope.launch {
            loadingDownloadViewModel.uiState.collect { state ->
                when (state) {
                    is LoadingDownloadUiState.Idle -> {
                        statusTextView.text = "Aguardando..."
                    }
                    is LoadingDownloadUiState.Loading -> {
                        statusTextView.text = state.message
                    }
                    is LoadingDownloadUiState.Success -> {
                        statusTextView.text = state.message
                        navigateProductScreen()
                    }
                    is LoadingDownloadUiState.Error -> {
                        statusTextView.text = "Erro: ${state.message}"
                    }
                }
            }
        }

        lifecycleScope.launch {
            val loggedUser = userDao.getAnyUserOnce()
            Timber.tag("LoadingDownload").d("Usuário encontrado no DB: ${loggedUser}")
            if (loggedUser?.isLogged == true) {
                navigateProductScreen()
                return@launch
            }

            statusTextView.text = "Iniciando processo completo..."

            val userId = loggedUser?.id
            val selectedMenuId = SessionPrefsManagerUtils.getSelectedMenuId()
            val selectedPosId = SessionPrefsManagerUtils.getPosId()
            val deviceId = SessionPrefsManagerUtils.getDeviceId()
            val cashierName = SessionPrefsManagerUtils.getOperatorName()

            if (selectedMenuId != null && selectedPosId != null && deviceId != null && cashierName != null && userId != null) {
                loadingDownloadViewModel.startCompleteProcess(
                    selectedMenuId,
                    selectedPosId,
                    deviceId,
                    cashierName,
                    userId
                )
            } else {
                val missingDataMessages = arrayOf(
                    "Dados insuficientes para iniciar o processo.",
                    "selectedMenuId: $selectedMenuId",
                    "selectedPosId: $selectedPosId",
                    "deviceId: $deviceId",
                    "cashierName: $cashierName",
                    "userId: $userId"
                )
                Timber.tag("LoadingDownload").d(missingDataMessages.joinToString())
                statusTextView.text = missingDataMessages.joinToString("\n")
            }
        }
    }

    private fun navigateProductScreen() {
        val intent = Intent(this, ProductsListActivity::class.java)
        startActivity(intent)
        finish()
    }
}