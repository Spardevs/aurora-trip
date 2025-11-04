package br.com.ticpass.pos.data.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.model.Menu
import br.com.ticpass.pos.util.ThumbnailManager
import br.com.ticpass.pos.view.ui.login.MenuScreen
import br.com.ticpass.pos.view.ui.login.PosScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MenuActivity : BaseActivity() {
    @Inject lateinit var apiRepository: APIRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menus)

        val recyclerView = findViewById<RecyclerView>(R.id.menusRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        loadMenus()
    }

    private fun loadMenus() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPref.getString("auth_token", null) ?: run {
            showErrorAndFinish("Token de autenticação não encontrado")
            return
        }
        val userId = sharedPref.getInt("user_id", -1).takeIf { it != -1 } ?: run {
            showErrorAndFinish("ID do usuário não encontrado")
            return
        }

        lifecycleScope.launch {
            try {
                val events = apiRepository.getEvents(
                    user = userId.toString(),
                    jwt = authToken
                )

                val menus = events.result.items.map { event ->
                    Menu(
                        id = event.id,
                        name = event.name,
                        imageUrl = event.ticket,
                        dateStart = event.dateStart,
                        dateEnd = event.dateEnd,
                        details = event.details,
                        mode = event.mode,
                        pin = event.pin
                    )
                }

                showMenus(menus)
            } catch (e: Exception) {
                Log.e("MenuActivity", "Error loading menus", e)
                showFallbackMenus()
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

    private fun showFallbackMenus() {
        val fallbackMenus = listOf(
            Menu(
                id = "1",
                name = "Evento 1",
                imageUrl = "",
                dateStart = "2024-01-01T00:00:00.000Z",
                dateEnd = "2024-12-31T23:59:59.000Z",
                details = "Detalhes do evento 1",
                mode = "1",
                pin = "1234",
            )
        )
        showMenus(fallbackMenus)
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
        val sessionPref = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val jwt = sessionPref.getString("auth_token", "") ?: ""

        return ThumbnailManager.downloadAndExtractThumbnails(
            context = this,
            menuId = menuId
        ) {
            apiRepository.downloadAllProductThumbnails(menuId, jwt)
        }
    }
}
