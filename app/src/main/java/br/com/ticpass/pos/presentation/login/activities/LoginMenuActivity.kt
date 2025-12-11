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
import br.com.ticpass.pos.core.util.SessionPrefsManagerUtils
import br.com.ticpass.pos.domain.menu.model.Menu
import br.com.ticpass.pos.presentation.login.adapters.LoginMenuAdapter
import br.com.ticpass.pos.presentation.login.states.LoginMenuUiState
import br.com.ticpass.pos.presentation.login.viewmodels.LoginMenuViewModel
import br.com.ticpass.pos.presentation.login.viewmodels.MenuLogoViewModel
import br.com.ticpass.pos.presentation.common.LoginLoadingFragment
import br.com.ticpass.pos.presentation.login.fragments.LoginPosFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class LoginMenuActivity : AppCompatActivity(), LoginLoadingFragment.Listener {

    private val menuViewModel: LoginMenuViewModel by viewModels()
    private val logoViewModel: MenuLogoViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private var menus: List<Menu> = emptyList()
    private var logoFiles: Map<String, File> = emptyMap()
    private var loadingFragment: LoginLoadingFragment? = null
    private var expectedLogoCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_menu)

        SessionPrefsManagerUtils.init(this)

        setupViews()
        observeViewModels()

        loadingFragment = LoginLoadingFragment.newInstance(
            getString(R.string.loading_default),
            cancelable = true
        )
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
        // Observa menus
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

                            // Dispara downloads de logo para cada menu que tiver logo
                            menus.forEach { menu ->
                                menu.logo?.let { rawLogo ->
                                    Timber.d("Activity -> forcing download for menu=${menu.id} rawLogo=$rawLogo")
                                    menuViewModel.downloadLogoForMenu(menu.id, rawLogo)
                                }
                            }

                            // Fallback: garante que o loading suma mesmo se algumas logos falharem
                            lifecycleScope.launch {
                                // Ajuste o tempo conforme a UX desejada
                                delay(2000)
                                removeLoadingFragmentIfExists()
                            }
                        } else {
                            removeLoadingFragmentIfExists()
                        }
                    }
                    is LoginMenuUiState.Error -> {
                        loadingFragment?.updateMessage(state.message)
                        loadingFragment?.showProgress(false)
                        removeLoadingFragmentIfExists()
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

        // Observa logos locais (apenas pra atualizar o mapa de logos e UI)
        lifecycleScope.launch {
            logoViewModel.localLogosState.collect { files ->
                logoFiles = files.associateBy { file ->
                    file.name.replace("logo_", "").replace(".png", "")
                }
                if (menus.isNotEmpty()) {
                    showMenus()
                }

                // Lógica anterior de remover loading com base em logos baixadas
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
        logoViewModel.loadAllLocalLogos()
    }

    private fun showMenus() {
        val adapter = LoginMenuAdapter(
            menus,
            onRequestLogo = { menuId, rawLogo ->
                menuViewModel.downloadLogoForMenu(menuId, rawLogo)
            },
            logos = logoFiles,
            onClick = { selectedMenu -> onMenuClicked(selectedMenu) }
        )
        recyclerView.adapter = adapter
    }

    private fun onMenuClicked(menu: Menu) {
        Timber.tag("MenuActivity").d("Menu selecionado: ${menu.id}")

        SessionPrefsManagerUtils.saveSelectedMenuId(menu.id)
        SessionPrefsManagerUtils.saveMenuStartDate(menu.date.start)
        SessionPrefsManagerUtils.saveMenuEndDate(menu.date.end)
        SessionPrefsManagerUtils.saveMenuName(menu.label)

        val posFragment = LoginPosFragment.newInstance(menu.id)

        val containerId = resources.getIdentifier("pos_fragment_container", "id", packageName)
        if (containerId != 0) {
            supportFragmentManager.beginTransaction()
                .replace(containerId, posFragment)
                .addToBackStack("pos")
                .commitAllowingStateLoss()
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, posFragment)
            .addToBackStack("pos")
            .commitAllowingStateLoss()
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

    override fun onLoadingCancelled() {
        removeLoadingFragmentIfExists()
    }

    override fun onLoadingAction(action: String) {
        if (action.equals("retry", ignoreCase = true)) {
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