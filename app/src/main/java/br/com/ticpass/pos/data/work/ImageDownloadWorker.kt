package br.com.ticpass.pos.data.work

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.ticpass.pos.data.api.APIRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class ImageDownloadWorker @Inject constructor(
    @ApplicationContext context: Context,
    workerParams: WorkerParameters,
    private val apiRepository: APIRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val menuId = inputData.getString("menuId") ?: return@withContext Result.failure()
            val destinationDir = inputData.getString("destinationDir") ?: return@withContext Result.failure()

            // Obter o token JWT do SharedPreferences
            val sharedPref = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPref.getString("auth_token", null) ?: return@withContext Result.failure()

            // Chamar a API para baixar todas as thumbnails
            val response = apiRepository.downloadAllProductThumbnails(menuId, authToken)

            if (response.isSuccessful) {
                val responseBody = response.body() ?: return@withContext Result.failure()

                // Criar diretório se não existir
                val productsDir = File(destinationDir)
                if (!productsDir.exists()) {
                    productsDir.mkdirs()
                }

                // Limpar diretório antes de salvar novos arquivos
                productsDir.listFiles()?.forEach { it.delete() }

                // Salvar a imagem (assumindo que a resposta é um único arquivo zip ou imagem)
                // Se for múltiplas imagens, você precisará ajustar esta parte
                val file = File(productsDir, "product_thumbnails_$menuId.jpg")
                saveResponseBodyToFile(responseBody, file)

                Log.d("ImageDownloadWorker", "Imagens salvas com sucesso em: ${file.absolutePath}")
                Result.success()
            } else {
                Log.e("ImageDownloadWorker", "Falha no download: ${response.code()}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("ImageDownloadWorker", "Erro ao baixar imagens", e)
            Result.failure()
        }
    }

    private fun saveResponseBodyToFile(body: ResponseBody, file: File) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }
}