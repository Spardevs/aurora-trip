package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.ApiRepository
import br.com.ticpass.pos.util.DeviceUtils
import br.com.ticpass.pos.util.ThumbnailManager
import br.com.ticpass.pos.viewmodel.login.LoginConfirmViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LoadingDownloadFragmentActivity : AppCompatActivity() {

    @Inject
    lateinit var apiRepository: ApiRepository

    private val loginConfirmViewModel: LoginConfirmViewModel by viewModels()
    private lateinit var tvDownloadProgress: TextView

    private val sessionPref by lazy { getSharedPreferences("SessionPrefs", MODE_PRIVATE) }
    private val userPref by lazy { getSharedPreferences("UserPrefs", MODE_PRIVATE) }

    // Dados existentes
    private val posId: String by lazy { sessionPref.getString("pos_id", "") ?: "" }
    private val nameOperator: String by lazy { userPref.getString("operator_name", "") ?: "" }

    // Tokens da API v2 (ajuste as keys se necessário)
    private val posAccessToken: String by lazy { userPref.getString("auth_token", "") ?: "" }
    private val proxyCredentials: String by lazy {
        // Se você tiver proxy credentials separado, use aqui
        // Caso contrário, pode ser o mesmo token ou vazio
        sessionPref.getString("proxy_credentials", "") ?: ""
    }

    // Menu selecionado
    private val menuId: String by lazy {
        val value = sessionPref.all["selected_menu_id"]
        when (value) {
            is String -> value
            is Int -> value.toString()
            else -> ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_loading_download)

        tvDownloadProgress = findViewById(R.id.tvDownloadProgress)

        lifecycleScope.launch {
            try {
                // 1) Validar dados mínimos
                if (posAccessToken.isBlank()) {
                    updateProgress("Erro: token de acesso inválido.")
                    Log.e("LoadingDownload", "posAccessToken vazio")
                    return@launch
                }
                if (menuId.isBlank()) {
                    updateProgress("Erro: nenhum menu selecionado.")
                    Log.e("LoadingDownload", "menuId vazio")
                    return@launch
                }
                if (posId.isBlank()) {
                    updateProgress("Erro: POS não configurado.")
                    Log.e("LoadingDownload", "posId vazio")
                    return@launch
                }

                // 2) Buscar menu
                updateProgress("Baixando menu")
                val menuResponse = apiRepository.getMenu()
                if (!menuResponse.isSuccessful) {
                    updateProgress("Erro ao baixar menu: HTTP=${menuResponse.code()}")
                    Log.e("LoadingDownload", "getMenu falhou: code=${menuResponse.code()}")
                    return@launch
                }

                val menuList = menuResponse.body()
                val selectedMenu = menuList?.edges?.find { it.id == menuId }
                if (selectedMenu == null) {
                    updateProgress("Menu selecionado não encontrado.")
                    Log.e("LoadingDownload", "Menu $menuId não encontrado")
                    return@launch
                }

                // 3) Baixar logo do menu (se existir)
                selectedMenu.logo?.let { logoId ->
                    updateProgress("Baixando logo do menu")
                    val logoFile = apiRepository.downloadMenuLogo(logoId)
                    if (logoFile != null) {
                        Log.d("LoadingDownload", "Logo baixada: ${logoFile.absolutePath}")
                    }
                }

                // 4) Buscar menu-pos
                updateProgress("Baixando configurações de POS")
                val menuPosResponse = apiRepository.getMenuPos(
                    menu = menuId,
                    take = 15,
                    page = 1,
                    available = "both"
                )
                if (!menuPosResponse.isSuccessful) {
                    updateProgress("Erro ao baixar menu POS: HTTP=${menuPosResponse.code()}")
                    Log.e("LoadingDownload", "getMenuPos falhou: code=${menuPosResponse.code()}")
                    return@launch
                }

                // 5) Abrir sessão do POS
                updateProgress("Abrindo sessão do POS")

                val deviceSerial = DeviceUtils.getDeviceSerial(this@LoadingDownloadFragmentActivity)
                val deviceMongoId = sessionPref.getString("device_mongo_id", null) // Obter o ID do MongoDB do dispositivo salvo anteriormente
                val cashierName = nameOperator.ifBlank { "Operador" }

                // Se proxyCredentials estiver vazio, você pode usar o próprio token ou deixar vazio
                val credentials = proxyCredentials.ifBlank { posAccessToken }

                // Verificar se temos o deviceMongoId salvo
                if (deviceMongoId.isNullOrBlank()) {
                    updateProgress("Erro: dispositivo não registrado corretamente.")
                    Log.e("LoadingDownload", "deviceMongoId não encontrado nas SharedPreferences")
                    return@launch
                }

                val openSessionResponse = apiRepository.openPosSession(
                    posAccessToken = posAccessToken,
                    proxyCredentials = credentials,
                    pos = posId,
                    device = deviceMongoId, // Usar o ID do MongoDB em vez do serial
                    cashier = cashierName
                )

                if (!openSessionResponse.isSuccessful) {
                    val errorBody = openSessionResponse.errorBody()?.string()
                    updateProgress("Falha ao abrir POS: HTTP=${openSessionResponse.code()}")
                    Log.e("LoadingDownload", "openPosSession falhou: code=${openSessionResponse.code()} body=$errorBody")
                    return@launch
                }

                val openSessionBody = openSessionResponse.body()
                if (openSessionBody == null) {
                    updateProgress("Falha ao abrir POS: resposta vazia.")
                    Log.e("LoadingDownload", "openPosSession body nulo")
                    return@launch
                }

                val sessionId = openSessionBody.id
                Log.d("LoadingDownload", "POS session aberta: sessionId=$sessionId")

                // ✅ SALVAR sessionId no SharedPreferences
                sessionPref.edit().putString("pos_session_id", sessionId).apply()

                // 6) Baixar produtos da sessão
                updateProgress("Baixando produtos do POS")
                val productsResponse = apiRepository.getPosSessionProducts(
                    posAccessToken = posAccessToken,
                    proxyCredentials = credentials
                )

                if (!productsResponse.isSuccessful) {
                    updateProgress("Erro ao baixar produtos: HTTP=${productsResponse.code()}")
                    Log.e("LoadingDownload", "getPosSessionProducts falhou: code=${productsResponse.code()}")
                    return@launch
                }

                val productsBody = productsResponse.body()
                if (productsBody == null || productsBody.products.isEmpty()) {
                    updateProgress("Nenhum produto encontrado.")
                    Log.w("LoadingDownload", "Lista de produtos vazia")
                } else {
                    Log.d("LoadingDownload", "Produtos recebidos: ${productsBody.products.size}")
                    // TODO: salvar produtos no banco local (Room)
                }

                // 7) Baixar thumbnails
                updateProgress("Baixando imagens dos produtos")
                try {
                    val thumbnailFile = apiRepository.downloadAllProductThumbnails(
                        menuId = menuId,
                        posAccessToken = posAccessToken,
                        proxyCredentials = credentials
                    )
                    if (thumbnailFile != null) {
                        Log.d("LoadingDownload", "Thumbnails baixadas: ${thumbnailFile.absolutePath}")
                        // Se quiser extrair o ZIP, use ThumbnailManager aqui
                    }
                } catch (e: Exception) {
                    Log.w("LoadingDownload", "Erro ao baixar thumbnails", e)
                }

                // 8) Confirmar login / salvar dados locais
                updateProgress("Salvando dados localmente")
                loginConfirmViewModel.confirmLogin(sessionPref, userPref)

                // 9) Navegar para ProductsActivity
                updateProgress("Abrindo POS")
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@LoadingDownloadFragmentActivity, ProductsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                Log.e("LoadingDownload", "Erro no fluxo de download", e)
                updateProgress("Erro: ${e.message}")
            }
        }
    }

    private suspend fun updateProgress(message: String) {
        withContext(Dispatchers.Main) {
            tvDownloadProgress.text = message
        }
    }
}