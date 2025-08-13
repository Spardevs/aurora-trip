package br.com.ticpass.pos.util

import android.content.Context
import android.util.Log
import retrofit2.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.invoke

object ThumbnailManager {
    private const val THUMBNAILS_DIR = "thumbnails"
    private const val THUMBNAILS_ZIP = "thumbnails.zip"

    fun getThumbnailFile(context: Context, imageName: String): File? {
        val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR)
        val thumbnailFile = File(thumbnailsDir, imageName)
        return if (thumbnailFile.exists()) thumbnailFile else null
    }

    suspend fun downloadAndExtractThumbnails(
        context: Context,
        menuId: String,
        downloader: suspend () -> Response<ResponseBody>
    ): Boolean {
        return try {
            val response = downloader()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    extractThumbnails(context, body.byteStream())
                    true
                } ?: false
            } else {
                Log.e("ThumbnailManager", "Failed to download thumbnails: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("ThumbnailManager", "Error downloading thumbnails", e)
            false
        }
    }
    private fun extractThumbnails(context: Context, zipStream: InputStream) {
        val zipFile = File(context.filesDir, THUMBNAILS_ZIP)
        val targetDir = File(context.filesDir, THUMBNAILS_DIR)

        try {
            FileOutputStream(zipFile).use { output ->
                zipStream.copyTo(output)
            }

            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(targetDir, entry.name)
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
        } finally {
            zipFile.delete()
        }
    }

    fun clearThumbnails(context: Context) {
        val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR)
        if (thumbnailsDir.exists()) {
            thumbnailsDir.deleteRecursively()
        }
    }
}