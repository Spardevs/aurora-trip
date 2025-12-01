package br.com.ticpass.pos.presentation.login.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.core.util.SessionPrefsManagerUtils
import br.com.ticpass.pos.data.menu.local.dao.MenuDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class LoadingDownloadFragmentActivity : AppCompatActivity() {

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

        // Verificar se há usuário logado
        lifecycleScope.launch {
            val loggedUser = userDao.getLoggedUser()
            if (loggedUser != null) {
                // Usuário já logado, pular login e download
                runOnUiThread {
                    statusTextView.text = "Usuário já logado. Carregando app..."
                }
                navigateProductScreen()
                return@launch
            }

            // Continuar com o fluxo normal
            statusTextView.text = "Iniciando download..."

            // MENU SELECT
            val selectedMenuId = SessionPrefsManagerUtils.getSelectedMenuId()
            selectedMenuId?.let { menuId ->
                CoroutineScope(Dispatchers.IO).launch {
                    menuDao.selectMenu(menuId, true)
                    updateStatusText(statusTextView, "Menu selecionado: $menuId")
                }
            }

            // POS SELECT
            val selectedPosId = SessionPrefsManagerUtils.getPosId()
            val cashierName = SessionPrefsManagerUtils.getOperatorName() ?: "Operador"
            val deviceSerial = SessionPrefsManagerUtils.getDeviceSerial() ?: "0000"
            selectedPosId?.let { posId ->
                CoroutineScope(Dispatchers.IO).launch {
                    posDao.selectPos(posId, true)
                    updateStatusText(statusTextView, "Pos selecionado: $posId")

                    posApiService.openPosSession(
                        OpenSessionRequest(
                            posId,
                            deviceSerial,
                            cashierName
                        )
                    )
                    updateStatusText(statusTextView, "Abrindo Caixa...")

                    // Download de produtos, categorias e thumbnails
                    downloadProductData(statusTextView, selectedMenuId)
                }
            }

            // USER SELECT
            val selectedUserId = userDao.getAnyUserOnce()
            selectedUserId?.let { userId ->
                CoroutineScope(Dispatchers.IO).launch {
                    userDao.updateUserLogged(userId.toString(), true)
                    updateStatusText(statusTextView, "Logando usuário")
                }
            }
        }
    }

    private fun updateStatusText(textView: TextView, text: String) {
        runOnUiThread {
            textView.text = text
        }
    }

    private fun downloadProductData(statusTextView: TextView, menuId: String?) {
        menuId?.let { id ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Download de categorias
                    updateStatusText(statusTextView, "Baixando categorias...")
                    categoryRepository.refreshCategories(id)

                    // Download de produtos
                    updateStatusText(statusTextView, "Baixando produtos...")
                    productRepository.refreshProducts(id)

                    // Download e extração de thumbnails
                    updateStatusText(statusTextView, "Baixando thumbnails...")
                    val thumbnailsDir = File(getExternalFilesDir(null), "ProductsThubnails")
                    productRepository.downloadAndExtractThumbnails(id, thumbnailsDir)

                    updateStatusText(statusTextView, "Download completo!")
                } catch (e: Exception) {
                    Timber.tag("Download Products").e("Erro no download: ${e.message}")
                    updateStatusText(statusTextView, "Erro no download: ${e.message}")
                }
            }
        }
    }

    private fun navigateProductScreen() {
        val intent = Intent(this, ProductsListActivity::class.java) // Substitua pelo nome correto da sua activity principal
        startActivity(intent)
        finish()
    }
}