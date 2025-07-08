package br.com.ticpass.pos.data.work

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import br.com.ticpass.pos.data.api.APIRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class ImageDownloadWorker(
    @ApplicationContext context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @Inject
    lateinit var apiRepository: APIRepository

    override suspend fun doWork(): Result {
        return try {
            val menuId = inputData.getString("menuId") ?: return Result.failure()
            val destinationDir = inputData.getString("destinationDir") ?: return Result.failure()
            val jwt = inputData.getString("jwt") ?: return Result.failure()

            val response = apiRepository.downloadAllProductThumbnails(menuId, jwt)

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    saveImageToStorage(body, destinationDir, "products_$menuId.jpg")
                }
                Result.success()
            } else {
                Log.e("ImageDownloadWorker", "Falha no download: ${response.errorBody()}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("ImageDownloadWorker", "Erro durante o download", e)
            Result.failure()
        }
    }

    private suspend fun saveImageToStorage(
        body: ResponseBody,
        destinationDir: String,
        fileName: String
    ) = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val file = File(destinationDir, fileName)
            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            Log.d("ImageDownloadWorker", "Imagem salva em: ${file.absolutePath}")
        } finally {
            inputStream?.close()
            outputStream?.close()
            // Liberar recursos da resposta
            body.close()
        }
    }
}