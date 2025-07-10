package br.com.ticpass.pos.data.activity

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.model.Menu
import br.com.ticpass.pos.view.ui.login.MenuScreen
import br.com.ticpass.pos.view.ui.login.PosScreen
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
                        onMenuClicked(
                            menuId     = selectedMenu.id,
                            menuName   = selectedMenu.name,
                            dateStart  = selectedMenu.dateStart,
                            dateEnd    = selectedMenu.dateEnd
                        )
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
                        onMenuClicked(selectedMenu.id, selectedMenu.name, selectedMenu.dateStart, selectedMenu.dateEnd)
                    }
                }
            }
        }
    }

    private fun onMenuClicked(
        menuId: String,
        menuName: String?    = null,
        dateStart: String?   = null,
        dateEnd: String?     = null
    ) {
        Log.d("MenuActivity", "Iniciando download das imagens para o menu: $menuId")

        val sessionPref = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val jwt = sessionPref.getString("auth_token", "") ?: ""

        with(sessionPref.edit()) {
            putString("selected_menu_id",        menuId)
            putString("selected_menu_name",      menuName)
            putString("selected_menu_dateStart", dateStart)
            putString("selected_menu_dateEnd",   dateEnd)
            apply()
        }

        lifecycleScope.launch {
            try {
                val response: Response<ResponseBody> =
                    apiRepository.downloadAllProductThumbnails(menuId, jwt)

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        withContext(Dispatchers.IO) {
                            val zipFile = File(filesDir, "thumbnails.zip")
                            zipFile.outputStream().use { output ->
                                body.byteStream().copyTo(output)
                            }

                            val targetDir = File(filesDir, "thumbnails")
                            unzip(zipFile, targetDir)

                            zipFile.delete()
                        }
                    }
                } else {
                    Log.e("MenuActivity", "Falha ao baixar ZIP: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MenuActivity", "Erro no download/descompactação", e)
            }

            startActivity(PosScreen.newIntent(this@MenuActivity, menuId))
        }
    }

    private fun unzip(zipFile: File, targetDirectory: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDirectory, entry.name)

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
