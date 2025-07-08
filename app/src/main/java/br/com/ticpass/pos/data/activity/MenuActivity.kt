package br.com.ticpass.pos.data.activity

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.model.Menu
import br.com.ticpass.pos.data.work.ImageDownloadWorker
import br.com.ticpass.pos.view.ui.login.MenuScreen
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MenuActivity : AppCompatActivity() {
    @Inject
    lateinit var apiRepository: APIRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menus)

        val recyclerView = findViewById<RecyclerView>(R.id.menusRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPref.getString("auth_token", null)
        val refreshToken = sharedPref.getString("refresh_token", null)
        val userId = sharedPref.getInt("user_id", -1)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = apiRepository.getEvents(
                    user = userId.toString(),
                    jwt = authToken.toString(),
                )

                val menus = events.result.items.map { event ->
                    Menu(
                        id = event.id,
                        name = event.name,
                        imageUrl = event.ticket,
                        dateStart = event.dateStart,
                        dateEnd = event.dateEnd,
                        details = event.details
                    )
                }

                withContext(Dispatchers.Main) {
                    recyclerView.adapter = MenuScreen(menus = menus) { selectedMenu ->
                        onMenuClicked(selectedMenu.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val fallbackMenus = listOf(
                        Menu(
                            id = "1",
                            name = "Evento 1",
                            imageUrl = "",
                            dateStart = "2024-01-01T00:00:00.000Z",
                            dateEnd = "2024-12-31T23:59:59.000Z"
                        )
                    )
                    recyclerView.adapter = MenuScreen(fallbackMenus) { selectedMenu ->
                        onMenuClicked(selectedMenu.id)
                    }
                }
            }
        }
    }

    private fun onMenuClicked(menuId: String) {
        Log.d("MenuActivity", "Iniciando download das imagens para o menu: $menuId")

        val productsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "products"
        )
        if (!productsDir.exists()) {
            productsDir.mkdirs()
        }

        val inputData = workDataOf(
            "menuId" to menuId,
            "destinationDir" to productsDir.absolutePath
        )

        val downloadWorkRequest = OneTimeWorkRequestBuilder<ImageDownloadWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueue(downloadWorkRequest)
    }
}