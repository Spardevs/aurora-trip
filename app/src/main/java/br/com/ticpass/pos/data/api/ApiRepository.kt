package br.com.ticpass.pos.data.api

import android.content.Context
import android.util.Log
import br.com.ticpass.pos.data.network.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject

class ApiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val service: ApiService,
    private val tokenManager: TokenManager
) {

    private fun buildCookieHeader(): String {
        val accessToken = tokenManager.getAccessToken()
        return if (accessToken.isNotBlank()) {
            "access=$accessToken"
        } else {
            ""
        }
    }

    suspend fun signInShortLived(
        shortLivedToken: String,
        pin: String
    ): Response<LoginResponse> {
        return try {
            val cookie = "shortLived=$shortLivedToken;"
            val authorization = pin
            val emptyJson = "{}".toRequestBody("application/json".toMediaType())

            Timber.tag("ApiRepository").d("Cookie: $cookie")
            Timber.tag("ApiRepository").d("Authorization: $authorization")

            val response = service.signInShortLived(
                cookie = cookie,
                authorization = authorization,
                body = emptyJson
            )

            Timber.tag("ApiRepository")
                .d("SignIn HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao fazer signIn")
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

            Timber.tag("ApiRepository")
                .d("SignIn(email/password) HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao fazer signIn(email/password)")
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

            Timber.tag("ApiRepository")
                .d("Registering device: serial=$serial, acquirer=$acquirer, variant=$variant")

            val response = service.registerDevice(
                authorization = proxyCredentials,
                body = body
            )

            Timber.tag("ApiRepository").d("RegisterDevice HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao registrar dispositivo")
            throw e
        }
    }

    suspend fun getMenu(
        take: Int = 10,
        page: Int = 1
    ): Response<MenuListResponse> {
        return try {
            Timber.tag("ApiRepository")
                .d("Fetching menu take=$take page=$page (headers via Interceptor)")
            val response = service.getMenu(
                take = take,
                page = page
            )
            Timber.tag("ApiRepository").d("GetMenu HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao buscar menus")
            throw e
        }
    }

    suspend fun getMenuPos(
        take: Int = 10,
        page: Int = 1,
        menu: String,
        available: String = "both"
    ): Response<MenuPosListResponse> {
        return try {
            Timber.tag("ApiRepository")
                .d("Fetching menu-pos take=$take page=$page menu=$menu available=$available")
            val response = service.getMenuPos(
                take = take,
                page = page,
                menu = menu,
                available = available
            )
            Timber.tag("ApiRepository")
                .d("GetMenuPos HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao buscar menu-pos")
            throw e
        }
    }

    suspend fun openPosSession(
        posAccessToken: String,
        proxyCredentials: String,
        pos: String,
        device: String,
        cashier: String
    ): Response<OpenPosSessionResponse> {
        return try {
            val cookie = buildCookieHeader()
            val payload = """
    {
    "pos": ${jsonEscape(pos)},
    "device": ${jsonEscape(device)},
    "cashier": ${jsonEscape(cashier)}
    }
    """.trimIndent()
            val body = payload.toRequestBody("application/json".toMediaType())

            Timber.tag("ApiRepository")
                .d("Opening POS session: pos=$pos, device=$device, cashier=$cashier")

            val response = service.openPosSession(
                cookie = cookie,
                authorization = proxyCredentials,
                body = body
            )

            Timber.tag("ApiRepository").d("OpenPosSession HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao abrir sessão POS")
            throw e
        }
    }

    // Alterado: agora recebe menuId e posAccessToken e chama /menu/products/pos?menu=<menuId>
    suspend fun getPosSessionProducts(
        menuId: String,
        posAccessToken: String
    ): Response<PosSessionProductsResponse> {
        return try {
            val cookie = buildCookieHeader()

            Timber.tag("ApiRepository").d("Fetching POS session products for menu=$menuId cookie=$cookie")

            val response = service.getPosSessionProducts(
                menu = menuId,
                cookie = cookie
            )

            Timber.tag("ApiRepository")
                .d("GetPosSessionProducts HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao buscar produtos da sessão POS")
            throw e
        }
    }

    suspend fun downloadAllProductThumbnails(
        menuId: String,
        posAccessToken: String,
        proxyCredentials: String
    ): File? {
        return try {
            val cookie = buildCookieHeader()
            val response = service.downloadAllProductThumbnails(
                menuId = menuId,
                cookie = cookie,
                authorization = proxyCredentials
            )

            if (response.isSuccessful) {
                val body = response.body() ?: return null
                val dir = File(context.filesDir, "ProductThumbnails")
                if (!dir.exists()) dir.mkdirs()
                val zipFile = File(dir, "${menuId}_all_thumbnails.zip")
                saveResponseBodyToFile(body, zipFile)

                // Pasta onde extrairemos os thumbnails (ProductThumbnails/<menuId>/)
                val extractedDir = File(dir, menuId)
                if (!extractedDir.exists()) extractedDir.mkdirs()

                // descompacta o zip para a pasta extraída
                try {
                    val extractedFiles = unzipToDirectory(zipFile, extractedDir)
                    // remover zip para economizar espaço
                    zipFile.delete()
                    Timber.tag("ApiRepository").d("Thumbnails descompactadas: ${extractedFiles.size} -> ${extractedDir.absolutePath}")
                } catch (ex: Exception) {
                    Timber.tag("ApiRepository").e(ex, "Erro ao descompactar thumbnails: ${ex.message}")
                }

                // retorna o diretório onde as imagens foram extraídas
                extractedDir
            } else {
                Timber.tag("ApiRepository")
                    .e("Erro download thumbnails menuId=$menuId code=${response.code()}")
                null
            }
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Exceção ao baixar thumbnails menuId=$menuId")
            null
        }
    }

    // ✅ Agora recebe logoId (não mais menuId)
    suspend fun downloadMenuLogo(logoId: String): File? {
        return try {
            val response = service.downloadMenuLogo(logoId)

            if (response.isSuccessful) {
                val body = response.body() ?: return null

                // Pasta MenusLogo dentro do storage interno da app
                val dir = File(context.filesDir, "MenusLogo")
                if (!dir.exists()) dir.mkdirs()

                // Nome do arquivo: <logoId>.png
                val file = File(dir, "$logoId.png")

                saveResponseBodyToFile(body, file)

                Timber.tag("ApiRepository").d("Logo baixada com sucesso: ${file.absolutePath}")
                file
            } else {
                Timber.tag("ApiRepository")
                    .e("Erro download logo logoId=$logoId code=${response.code()}")
                null
            }
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Exceção ao baixar logo logoId=$logoId")
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

    // Descompacta um zip para um diret�F3rio de destino e retorna a lista de arquivos extra�EDdos
    private fun unzipToDirectory(zipFile: File, destDir: File): List<File> {
        val extracted = mutableListOf<File>()
        if (!destDir.exists()) destDir.mkdirs()

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(8 * 1024)
            while (entry != null) {
                try {
                    if (entry.isDirectory) {
                        val dir = File(destDir, entry.name)
                        if (!dir.exists()) dir.mkdirs()
                    } else {
                        val entryName = entry.name
                        // toma apenas o nome do arquivo, ignorando eventuais pastas internas
                        val targetFile = File(destDir, entryName.substringAfterLast('/'))
                        targetFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

                        FileOutputStream(targetFile).use { fos ->
                            var count: Int
                            while (zis.read(buffer).also { count = it } != -1) {
                                fos.write(buffer, 0, count)
                            }
                            fos.flush()
                        }
                        extracted.add(targetFile)
                    }
                } finally {
                    zis.closeEntry()
                }
                entry = zis.nextEntry
            }
        }
        return extracted
    }

    suspend fun closePosSession(
        posAccessToken: String,
        proxyCredentials: String,
        sessionId: String
    ): Response<ClosePosSessionResponse> {
        return try {
            val cookie = buildCookieHeader()
            val payload = """
    {
    "id": ${jsonEscape(sessionId)}
    }
    """.trimIndent()
            val body = payload.toRequestBody("application/json".toMediaType())

            Timber.tag("ApiRepository").d("Closing POS session: id=$sessionId")

            val response = service.closePosSession(
                cookie = cookie,
                authorization = proxyCredentials,
                body = body
            )

            Timber.tag("ApiRepository")
                .d("ClosePosSession HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao fechar sessão POS")
            throw e
        }
    }

    suspend fun pingDeviceLocation(
        accessToken: String,
        proxyCredentials: String,
        serial: String,
        latitude: Double,
        longitude: Double
    ): Response<DevicePingResponse> {
        return try {
            val cookie = buildCookieHeader()

            val payload = """
    {
    "serial": ${jsonEscape(serial)},
    "latitude": $latitude,
    "longitude": $longitude
    }
    """.trimIndent()

            val body = payload.toRequestBody("application/json".toMediaType())

            Timber.tag("ApiRepository")
                .d("Ping device location: serial=$serial, lat=$latitude, lng=$longitude")

            val response = service.pingDeviceLocation(
                cookie = cookie,
                authorization = proxyCredentials,
                body = body
            )

            Timber.tag("ApiRepository").d("PingDeviceLocation HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao enviar ping de localização do dispositivo")
            throw e
        }
    }

    suspend fun syncMenuPosSession(
        posAccessToken: String,
        proxyCredentials: String,
        sessionId: String
    ): Response<SyncMenuPosSessionResponse> {
        return try {
            val cookie = buildCookieHeader()

            // Mesmo body do curl: todos os arrays vazios
            val payload = """
    {
    "orders": [],
    "payments": [],
    "acquisitions": [],
    "passes": [],
    "refunds": [],
    "consumptions": [],
    "giftcards": [],
    "redemptions": [],
    "cashups": []
    }
    """.trimIndent()

            val body = payload.toRequestBody("application/json".toMediaType())

            Timber.tag("ApiRepository").d("Syncing POS session: sessionId=$sessionId")

            val response = service.syncMenuPosSession(
                sessionId = sessionId,
                cookie = cookie,
                authorization = proxyCredentials,
                body = body
            )

            Timber.tag("ApiRepository")
                .d("SyncMenuPosSession HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao sincronizar sessão POS sessionId=$sessionId")
            throw e
        }
    }

    private fun isValidMongoId(id: String): Boolean {
        return Regex("^[a-fA-F0-9]{24}$").matches(id)
    }

    suspend fun getMenuCategoriesPos(
        menuId: String,
        posAccessToken: String
    ): Response<com.google.gson.JsonElement> {
        return try {
            val cookie = buildCookieHeader()
            Timber.tag("ApiRepository").d("Fetching menu categories POS (raw) for menu=$menuId cookie=$cookie")
            val response = service.getMenuCategoriesPos(menu = menuId, cookie = cookie)
            Timber.tag("ApiRepository")
                .d("GetMenuCategoriesPos (raw) HTTP=${response.code()} success=${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Timber.tag("ApiRepository").e(e, "Erro ao buscar categories da sessão POS (raw)")
            throw e
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