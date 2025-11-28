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
import br.com.ticpass.pos.domain.menu.model.MenuDb
import br.com.ticpass.pos.presentation.login.adapters.LoginMenuAdapter
import br.com.ticpass.pos.presentation.login.states.LoginMenuUiState

import br.com.ticpass.pos.core.util.SessionPrefsManager
import br.com.ticpass.pos.presentation.login.viewmodels.LoginMenuViewModel
import br.com.ticpass.pos.presentation.login.viewmodels.MenuLogoViewModel
import br.com.ticpass.pos.presentation.common.LoginLoadingFragment

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class LoginMenuActivity : AppCompatActivity(), LoginLoadingFragment.Listener {

    private val menuViewModel: LoginMenuViewModel by viewModels()
    private val logoViewModel: MenuLogoViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private var menus: List<MenuDb> = emptyList()
    private var logoFiles: Map<String, File> = emptyMap()

    // Loading fragment
    private var loadingFragment: LoginLoadingFragment? = null
    private var expectedLogoCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_menu)

        // Inicializa SessionPrefsManager
        SessionPrefsManager.init(this)

        setupViews()
        observeViewModels()

        // Mostra o fragment de loading antes de iniciar o carregamento
        loadingFragment = LoginLoadingFragment.newInstance(getString(R.string.loading_default), cancelable = true)
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, loadingFragment!!, "login_loading")
            .commitAllowingStateLoss()

        loadMenus()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.menusRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = LoginMenuAdapter(
            emptyList(),
            onRequestLogo = { _, _ -> /* noop */ },
            logos = emptyMap(),
            onClick = { /* noop */ }
        )
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            menuViewModel.uiState.collect { state ->
                when (state) {
                    is LoginMenuUiState.Loading -> {
                        loadingFragment?.updateMessage(getString(R.string.loading_menus))
                        loadingFragment?.showProgress(true)
                    }
                    is LoginMenuUiState.Success -> {
                        menus = state.menus
                        showMenus()
                        expectedLogoCount = menus.count { !it.logo.isNullOrEmpty() }

                        if (expectedLogoCount > 0) {
                            loadingFragment?.updateMessage(getString(R.string.downloading_logos))
                            loadingFragment?.showProgress(true)
                            // logos ser�o baixadas de forma lazy pelo Adapter quando as c�lulas aparecerem
                        } else {
                            removeLoadingFragmentIfExists()
                        }
                    }
                    is LoginMenuUiState.Error -> {
                        loadingFragment?.updateMessage(state.message)
                        loadingFragment?.showProgress(false)
                        showError(state.message)
                    }
                    is LoginMenuUiState.Empty -> {
                        val msg = try {
                            getString(R.string.no_menus_found)
                        } catch (e: Exception) {
                            "Nenhum menu encontrado"
                        }
                        loadingFragment?.updateMessage(msg)
                        loadingFragment?.showProgress(false)
                        removeLoadingFragmentIfExists()
                        showError(msg)
                    }
                }
            }
        }

        // Observa o estado das logos
        lifecycleScope.launch {
            logoViewModel.localLogosState.collect { files ->
                // Atualiza o mapa de logos
                logoFiles = files.associateBy { file ->
                    // Extrai o ID do menu do nome do arquivo
                    file.name.replace("logo_", "").replace(".png", "")
                }
                // Atualiza a lista se já estiver carregada
                if (menus.isNotEmpty()) {
                    showMenus()
                }

                // Se já tivermos todas as logos esperadas, remover o loading
                if (expectedLogoCount == 0) {
                    removeLoadingFragmentIfExists()
                } else if (files.size >= expectedLogoCount) {
                    removeLoadingFragmentIfExists()
                }
            }
        }
    }

    private fun loadMenus() {
        menuViewModel.loadMenuItems(take = 10, page = 1)
        // Carrega logos locais imediatamente (pode já conter algumas imagens)
        logoViewModel.loadAllLocalLogos()
    }

    private fun showMenus() {
        val adapter = LoginMenuAdapter(
            menus,
            onRequestLogo = { menuId, rawLogo ->
                // solicita o download lazy via ViewModel
                menuViewModel.downloadLogoForMenu(menuId, rawLogo)
            },
            logos = logoFiles,
            onClick = { selectedMenu -> onMenuClicked(selectedMenu) }
        )
        recyclerView.adapter = adapter
    }

    private fun downloadMenuLogos(menus: List<MenuDb>) {
        menus.forEach { menu ->
            menu.logo?.let { logoId ->
                logoViewModel.downloadLogo(logoId)
            }
        }
    }

    private fun onMenuClicked(menu: MenuDb) {
        Timber.tag("MenuActivity").d("Menu selecionado: ${menu.id}")

        // Salva o id selecionado no SessionPrefs
        SessionPrefsManager.saveSelectedMenuId(menu.id.toString())

        // Navega para PosActivity (descomente se quiser navegar)
//    val intent = PosActivity.newIntent(this, menu.id)
//    startActivity(intent)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun removeLoadingFragmentIfExists() {
        val frag = supportFragmentManager.findFragmentByTag("login_loading")
        if (frag != null) {
            supportFragmentManager.beginTransaction().remove(frag).commitAllowingStateLoss()
            loadingFragment = null
        }
    }

    // LoginLoadingFragment.Listener implementation
    override fun onLoadingCancelled() {
        removeLoadingFragmentIfExists()
    }

    override fun onLoadingAction(action: String) {
        // exemplo: action = "retry"
        if (action.equals("retry", ignoreCase = true)) {
            // reinicia o carregamento
            expectedLogoCount = 0
            loadingFragment?.updateMessage(getString(R.string.loading_default))
            loadingFragment?.showProgress(true)
            menuViewModel.loadMenuItems(take = 10, page = 1)
            logoViewModel.loadAllLocalLogos()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, LoginMenuActivity::class.java)
        }
    }
}