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
import br.com.ticpass.pos.presentation.menu.LoginMenuViewModel
import br.com.ticpass.pos.presentation.menu.MenuLogoViewModel

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class LoginMenuActivity : AppCompatActivity() {

    private val menuViewModel: LoginMenuViewModel by viewModels()
    private val logoViewModel: MenuLogoViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private var menus: List<MenuDb> = emptyList()
    private var logoFiles: Map<String, File> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_menu)

        // Inicializa SessionPrefsManager
        SessionPrefsManager.init(this)

        setupViews()
        observeViewModels()
        loadMenus()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.menusRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    private fun observeViewModels() {
        // Observa o estado dos menus
        lifecycleScope.launch {
            menuViewModel.uiState.collect { state ->
                when (state) {
                    is LoginMenuUiState.Loading -> {
                        // Mostrar loading
                    }
                    is LoginMenuUiState.Success -> {
                        menus = state.menus // lista de MenuDb
                        showMenus()
                        downloadMenuLogos(state.menus)
                    }
                    is LoginMenuUiState.Error -> {
                        showError(state.message)
                    }
                    is LoginMenuUiState.Empty -> {
                        // Tratar lista vazia
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
            }
        }
    }

    private fun loadMenus() {
        menuViewModel.loadMenuItems(take = 10, page = 1)
        logoViewModel.loadAllLocalLogos()
    }

    private fun showMenus() {
        val adapter = LoginMenuAdapter(menus, logoFiles) { selectedMenu ->
            onMenuClicked(selectedMenu)
        }
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

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, LoginMenuActivity::class.java)
        }
    }
}