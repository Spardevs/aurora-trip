package br.com.ticpass.pos.data.api

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class Api2Repository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val service: Api2Service
) {

    suspend fun signInShortLived(
        shortLivedToken: String,
        pin: String
    ): Response<LoginResponse> {
        return try {
            val cookie = "shortLived=$shortLivedToken;"
            val authorization = pin
            val emptyJson = "{}".toRequestBody("application/json".toMediaType())

            Log.d("Api2Repository", "Cookie: $cookie")
            Log.d("Api2Repository", "Authorization: $authorization")

            val response = service.signInShortLived(
                cookie = cookie,
                authorization = authorization,
                body = emptyJson
            )

            Log.d(
                "Api2Repository",
                "SignIn HTTP=${response.code()} success=${response.isSuccessful}"
            )
            response
        } catch (e: Exception) {
            Log.e("Api2Repository", "Erro ao fazer signIn", e)
            throw e
        }
    }

    suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Response<LoginResponse> {
        return try {
            val payload = """{"email": ${jsonEscape(email)}, "password": ${jsonEscape(password)}}"""
            val body = payload.toRequestBody("application/json".toMediaType())

            val response = service.signInWithEmailPassword(body = body)

            Log.d("Api2Repository", "SignIn(email/password) HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Log.e("Api2Repository", "Erro ao fazer signIn(email/password)", e)
            throw e
        }
    }

    suspend fun registerDevice(
        serial: String,
        acquirer: String,
        variant: String,
        proxyCredentials: String
    ): Response<RegisterDeviceResponse> {
        return try {
            val payload = """
    {
    "serial": ${jsonEscape(serial)},
    "acquirer": ${jsonEscape(acquirer)},
    "variant": ${jsonEscape(variant)}
    }
    """.trimIndent()
            val body = payload.toRequestBody("application/json".toMediaType())

            Log.d("Api2Repository", "Registering device: serial=$serial, acquirer=$acquirer, variant=$variant")

            val response = service.registerDevice(
                authorization = proxyCredentials,
                body = body
            )

            Log.d("Api2Repository", "RegisterDevice HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Log.e("Api2Repository", "Erro ao registrar dispositivo", e)
            throw e
        }
    }

    suspend fun getMenu(
        take: Int = 10,
        page: Int = 1
    ): Response<MenuListResponse> {
        return try {
            Log.d("Api2Repository", "Fetching menu take=$take page=$page (headers via Interceptor)")
            val response = service.getMenu(
                take = take,
                page = page
            )
            Log.d("Api2Repository", "GetMenu HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Log.e("Api2Repository", "Erro ao buscar menus", e)
            throw e
        }
    }

    suspend fun downloadMenuLogo(menuId: String): File? {
        return try {
            val response = service.downloadMenuLogo(menuId)

            if (response.isSuccessful) {
                val body = response.body() ?: return null

                // Pasta MenusLogo dentro do storage interno da app
                val dir = File(context.filesDir, "MenusLogo")
                if (!dir.exists()) dir.mkdirs()

                // Nome do arquivo: <menuId>.png
                val file = File(dir, "$menuId.png")

                saveResponseBodyToFile(body, file)

                Log.d("Api2Repository", "Logo baixada com sucesso: ${file.absolutePath}")
                file
            } else {
                Log.e("Api2Repository", "Erro download logo menuId=$menuId code=${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("Api2Repository", "Exceção ao baixar logo menuId=$menuId", e)
            null
        }
    }

    private fun saveResponseBodyToFile(body: ResponseBody, file: File) {
        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
    }

    // Helper simples para strings JSON
    private fun jsonEscape(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}