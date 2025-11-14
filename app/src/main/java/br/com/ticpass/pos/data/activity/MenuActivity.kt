package br.com.ticpass.pos.data.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.Api2Repository
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.api.ErrorResponse
import br.com.ticpass.pos.data.model.Menu
import br.com.ticpass.pos.util.ThumbnailManager
import br.com.ticpass.pos.view.ui.login.MenuScreen
import br.com.ticpass.pos.view.ui.login.PosScreen
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MenuActivity : BaseActivity() {
    @Inject lateinit var api2Repository: Api2Repository
    @Inject lateinit var apiRepository: APIRepository // Mantém para downloadThumbnails

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menus)

        val recyclerView = findViewById<RecyclerView>(R.id.menusRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        loadMenus()
    }

    private fun loadMenus() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val accessToken = sharedPref.getString("auth_token", null) ?: run {
            showErrorAndFinish("Token de autenticação não encontrado")
            return
        }
        val refreshToken = sharedPref.getString("refresh_token", null) ?: run {
            showErrorAndFinish("Refresh token não encontrado")
            return
        }

        lifecycleScope.launch {
            try {
                // ✅ USA API V2 PARA BUSCAR MENUS
                val resp = api2Repository.getMenu(take = 10, page = 1)

                if (!resp.isSuccessful) {
                    val code = resp.code()
                    val errorBody = resp.errorBody()?.string()
                    val errorMsg = try {
                        val err = Gson().fromJson(errorBody, ErrorResponse::class.java)
                        err?.message ?: "Erro desconhecido"
                    } catch (_: Exception) {
                        errorBody ?: "Erro desconhecido"
                    }

                    if (code == 401) {
                        showErrorAndFinish("Não autorizado: $errorMsg")
                    } else {
                        showErrorAndFinish("Falha ao carregar menus (HTTP $code): $errorMsg")
                    }
                    return@launch
                }

                val body = resp.body() ?: run {
                    showErrorAndFinish("Resposta vazia do servidor")
                    return@launch
                }

                val menus: List<Menu> = body.edges.map { edge ->
                    Menu(
                        id = edge.id,
                        name = edge.label,
                        imageUrl = "", // não veio no payload
                        dateStart = edge.date.start,
                        dateEnd = edge.date.end,
                        details = edge.pass.description,
                        mode = edge.mode,
                        pin = "" // não veio no payload
                    )
                }

                showMenus(menus)
            } catch (e: Exception) {
                Log.e("MenuActivity", "Error loading menus", e)
                showErrorAndFinish("Erro ao carregar menus: ${e.message}")
            }
        }
    }

    private fun showMenus(menus: List<Menu>) {
        val recyclerView = findViewById<RecyclerView>(R.id.menusRecyclerView)
        recyclerView.adapter = MenuScreen(menus = menus) { selectedMenu ->
            onMenuClicked(
                menuId = selectedMenu.id,
                menuName = selectedMenu.name,
                dateStart = selectedMenu.dateStart,
                dateEnd = selectedMenu.dateEnd,
                pin = selectedMenu.pin,
                details = selectedMenu.details,
                mode = selectedMenu.mode
            )
        }
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun onMenuClicked(
        menuId: String,
        menuName: String? = null,
        dateStart: String? = null,
        dateEnd: String? = null,
        pin: String? = null,
        details: String? = null,
        mode: String? = null,
        logo: String? = null
    ) {
        Log.d("MenuActivity", "Preparando menu: $menuId")

        saveMenuSession(menuId, menuName, dateStart, dateEnd, pin, details, mode, logo)

        lifecycleScope.launch {
            val success = downloadThumbnails(menuId)
            if (!success) {
                Log.w("MenuActivity", "Usando thumbnails locais ou placeholders")
            }
            startActivity(PosScreen.newIntent(this@MenuActivity, menuId))
        }
    }

    private fun saveMenuSession(
        menuId: String,
        menuName: String?,
        dateStart: String?,
        dateEnd: String?,
        pin: String?,
        details: String?,
        mode: String?,
        logo: String?
    ) {
        val sessionPref = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        sessionPref.edit().apply {
            putString("selected_menu_id", menuId)
            putString("selected_menu_name", menuName)
            putString("selected_menu_dateStart", dateStart)
            putString("selected_menu_dateEnd", dateEnd)
            putString("selected_menu_pin", pin)
            putString("selected_menu_details", details)
            putString("selected_menu_mode", mode)
            putString("selected_menu_logo", logo)
            apply()
        }
    }

    private suspend fun downloadThumbnails(menuId: String): Boolean {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val jwt = sharedPref.getString("auth_token", "") ?: ""

        return try {
            ThumbnailManager.downloadAndExtractThumbnails(
                context = this,
                menuId = menuId
            ) {
                apiRepository.downloadAllProductThumbnails(menuId, jwt)
            }
        } catch (e: Exception) {
            Log.e("MenuActivity", "Erro ao baixar thumbnails", e)
            false
        }
    }
}