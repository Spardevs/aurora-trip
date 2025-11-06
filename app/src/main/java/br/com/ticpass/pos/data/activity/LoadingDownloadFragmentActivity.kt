package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.viewmodel.login.LoginConfirmViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LoadingDownloadFragmentActivity : AppCompatActivity() {

    @Inject
    lateinit var apiRepository: br.com.ticpass.pos.data.api.APIRepository
    private val loginConfirmViewModel: LoginConfirmViewModel by viewModels()
    private lateinit var tvDownloadProgress: TextView
    private val sessionPref by lazy { getSharedPreferences("SessionPrefs", MODE_PRIVATE) }
    private val userPref by lazy { getSharedPreferences("UserPrefs", MODE_PRIVATE) }
    private val posId: String by lazy { sessionPref.getString("pos_id", "") ?: "" }
    private val jwt: String by lazy { userPref.getString("auth_token", "") ?: "" }
    private val nameOperator: String by lazy { userPref.getString("operator_name", "") ?: "" }
    private val eventId: String by lazy {
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

        val userIdNumber = userPref.getInt("user_id", 0)
        val userId = userIdNumber.toString()

        lifecycleScope.launch {
            try {
                updateProgress("Baixando menu")
                val eventsResponse = apiRepository.getEvents(
                    user = userId,
                    jwt = jwt
                )
                if (eventsResponse.status != 200) {
                    updateProgress("Erro ao baixar menu: ${eventsResponse.message}")
                    return@launch
                }

                updateProgress("Baixando produtos do evento")
                val productsResponse = apiRepository.getEventProducts(
                    event = eventId,
                    jwt = jwt
                )
                if (productsResponse.status != 200) {
                    updateProgress("Erro ao baixar produtos: ${productsResponse.message}")
                    return@launch
                }

                // Salvar evento selecionado no banco local
                updateProgress("Salvando evento selecionado")
                loginConfirmViewModel.confirmLogin(sessionPref, userPref)

                updateProgress("Abrindo POS")
                Log.d("LoadingDownloadFragmentActivity", "Abrindo POS: $posId; jwt: $jwt")
                val openPosResponse = apiRepository.openPos(posId, nameOperator, jwt)
                if (openPosResponse.status == 403) {
                    updateProgress("Permissão negada: não foi possível abrir este caixa.")
                    return@launch
                } else if (openPosResponse.status != 200) {
                    updateProgress("Falha ao abrir POS: ${openPosResponse.message}")
                    return@launch
                }

                updateProgress("Download concluído com sucesso")

                // Navegar para a tela de produtos após sucesso
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@LoadingDownloadFragmentActivity, ProductsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
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