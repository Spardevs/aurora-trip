package br.com.ticpass.pos.presentation.login.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.login.adapters.LoginPosAdapter
import br.com.ticpass.pos.presentation.login.states.LoginPosUiState
import br.com.ticpass.pos.presentation.login.viewmodels.LoginPosViewModel
import br.com.ticpass.pos.presentation.common.LoginLoadingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class LoginPosActivity : AppCompatActivity(), LoginLoadingFragment.Listener {

    private val posViewModel: LoginPosViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LoginPosAdapter

    // Loading fragment
    private var loadingFragment: LoginLoadingFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_pos)

        recyclerView = findViewById(R.id.pos_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        adapter = LoginPosAdapter(
            items = mutableListOf(),
            onClick = { pos ->
                // salva POS escolhido nas preferencias de sessão
                val prefs = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("pos_id", pos.id)
                    putString("pos_name", pos.name)
                    putLong("pos_commission", pos.commission.toLong())
                    apply()
                }
                // navegar para próxima tela, se houver
            },
            onLongClick = { pos ->
                // mostra diálogo de fechar (implemente Close via usecase se desejar)
                showClosePosDialog(pos)
            }
        )
        recyclerView.adapter = adapter

        // Recupera menuId e cabeçalhos (pos access token / proxy) das preferências
        val prefs = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val menuId = prefs.getString("selected_menu_id", null)
        val posAccessToken = prefs.getString("pos_access_token", "") ?: ""
        val proxyCredentials = prefs.getString("proxy_credentials", "") ?: ""

        if (menuId.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.no_menu_selected), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Mostra fragment de loading enquanto carrega os POS
        loadingFragment = LoginLoadingFragment.newInstance(getString(R.string.loading_default), cancelable = true)
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, loadingFragment!!, "login_loading_pos")
            .commitAllowingStateLoss()

        // Observa o estado do ViewModel e atualiza RecyclerView
        lifecycleScope.launch {
            posViewModel.uiState.collectLatest { state ->
                when (state) {
                    is LoginPosUiState.Loading -> {
                        // opcional: atualizar texto do loading fragment
                        loadingFragment?.updateMessage(getString(R.string.loading_pos))
                        loadingFragment?.showProgress(true)
                    }
                    is LoginPosUiState.Success -> {
                        removeLoadingFragmentIfExists()
                        adapter.setItems(state.posList)
                    }
                    is LoginPosUiState.Empty -> {
                        removeLoadingFragmentIfExists()
                        adapter.setItems(emptyList())
                        Toast.makeText(this@LoginPosActivity, getString(R.string.no_pos_found), Toast.LENGTH_SHORT).show()
                    }
                    is LoginPosUiState.Error -> {
                        removeLoadingFragmentIfExists()
                        Toast.makeText(this@LoginPosActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Inicia a observação e tentativa de refresh
        // Atenção à ordem: observeMenu(menuId, authorization, cookie)
        posViewModel.observeMenu(menuId, authorization = proxyCredentials, cookie = posAccessToken)
    }

    private fun removeLoadingFragmentIfExists() {
        val frag = supportFragmentManager.findFragmentByTag("login_loading_pos")
        if (frag != null) {
            supportFragmentManager.beginTransaction().remove(frag).commitAllowingStateLoss()
            loadingFragment = null
        }
    }

    private fun showClosePosDialog(pos: br.com.ticpass.pos.domain.pos.model.Pos) {
        Timber.d("Solicitado fechar POS id=${pos.id}")
        // ex.: abrir AlertDialog aqui ou chamar UseCase para fechar sessão
        Toast.makeText(this, "Fechar POS: ${pos.prefix} ${pos.sequence}", Toast.LENGTH_SHORT).show()
    }

    // LoginLoadingFragment.Listener
    override fun onLoadingCancelled() {
        removeLoadingFragmentIfExists()
    }

    override fun onLoadingAction(action: String) {
        if (action.equals("retry", ignoreCase = true)) {
            // reinicia carregamento
            loadingFragment = LoginLoadingFragment.newInstance(getString(R.string.loading_default), cancelable = true)
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, loadingFragment!!, "login_loading_pos")
                .commitAllowingStateLoss()

            val prefs = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
            val menuId = prefs.getString("selected_menu_id", null) ?: return
            val posAccessToken = prefs.getString("pos_access_token", "") ?: ""
            val proxyCredentials = prefs.getString("proxy_credentials", "") ?: ""
            posViewModel.observeMenu(menuId, authorization = proxyCredentials, cookie = posAccessToken)
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, LoginPosActivity::class.java)
    }
}