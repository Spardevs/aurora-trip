package br.com.ticpass.pos.presentation.login.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.core.util.SessionPrefsManagerUtils
import br.com.ticpass.pos.data.menu.local.dao.MenuDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.category.repository.CategoryRepositoryImpl
import br.com.ticpass.pos.data.pos.local.dao.PosDao
import br.com.ticpass.pos.data.pos.remote.service.OpenSessionRequest
import br.com.ticpass.pos.data.pos.remote.service.PosApiService
import br.com.ticpass.pos.data.product.repository.ProductRepositoryImpl
import br.com.ticpass.pos.data.user.local.dao.UserDao
import br.com.ticpass.pos.presentation.product.activities.ProductsListActivity
import br.com.ticpass.pos.presentation.login.viewmodels.LoadingDownloadViewModel
import br.com.ticpass.pos.presentation.login.viewmodels.LoadingDownloadUiState

@AndroidEntryPoint
class LoadingDownloadFragmentActivity : AppCompatActivity() {

    private val loadingDownloadViewModel: LoadingDownloadViewModel by viewModels()
    @Inject
    lateinit var menuDao: MenuDao
    @Inject
    lateinit var posDao: PosDao
    @Inject
    lateinit var posApiService: PosApiService
    @Inject
    lateinit var productRepository: ProductRepositoryImpl
    @Inject
    lateinit var categoryRepository: CategoryRepositoryImpl
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
            val loggedUser = userDao.getLoggedUser()
            if (loggedUser != null) {
                navigateProductScreen()
                return@launch
            }

            statusTextView.text = "Iniciando download..."

            val selectedMenuId = SessionPrefsManagerUtils.getSelectedMenuId()
            selectedMenuId?.let { menuId ->
                loadingDownloadViewModel.startDownload(menuId)
            }
            val selectedPosId = SessionPrefsManagerUtils.getPosId()?: return@launch
            val deviceSerial = SessionPrefsManagerUtils.getDeviceSerial()?: return@launch
            val cashierName = SessionPrefsManagerUtils.getOperatorName()?: return@launch
            loadingDownloadViewModel.startOpenPos(selectedPosId, deviceSerial, cashierName)

        }

    }

     fun updateStatusText(textView: TextView, text: String) {
        runOnUiThread {
            textView.text = text
        }
    }
     fun navigateProductScreen() {
        val intent = Intent(this, ProductsListActivity::class.java) // Ajuste para a Activity correta
        startActivity(intent)
        finish()
    }
}