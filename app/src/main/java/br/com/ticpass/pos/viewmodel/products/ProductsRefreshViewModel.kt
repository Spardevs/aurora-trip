package br.com.ticpass.pos.viewmodel.products

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.data.api.ApiRepository
import br.com.ticpass.pos.data.room.dao.CategoryDao
import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.CategoryEntity
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.entity.ProductEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import com.google.gson.JsonElement

@HiltViewModel
class ProductsRefreshViewModel @Inject constructor(
    private val apiRepository: ApiRepository,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val eventDao: EventDao
) : ViewModel() {

    // helper: extrai id/string de JsonElement? (category/menu etc.)
    private fun extractIdFromJson(el: JsonElement?): String {
        if (el == null) return ""
        return try {
            if (el.isJsonNull) return ""
            if (el.isJsonPrimitive) return el.asString
            val obj = el.asJsonObject
            when {
                obj.has("id") -> obj.get("id").asString
                obj.has("_id") -> obj.get("_id").asString
                obj.has("label") -> obj.get("label").asString
                obj.has("name") -> obj.get("name").asString
                obj.entrySet().firstOrNull()?.value?.isJsonPrimitive == true ->
                    obj.entrySet().first().value.asString
                else -> obj.toString()
            }
        } catch (ex: Exception) {
            el.toString()
        }
    }

    // helper: extrai um nome legível de JsonElement? (label/name/title/fallback)
    private fun extractNameFromJson(el: JsonElement?): String {
        if (el == null) return ""
        return try {
            if (el.isJsonNull) return ""
            if (el.isJsonPrimitive) return el.asString
            val obj = el.asJsonObject
            when {
                obj.has("label") -> obj.get("label").asString
                obj.has("name") -> obj.get("name").asString
                obj.has("title") -> obj.get("title").asString
                else -> obj.entrySet().firstOrNull { it.value.isJsonPrimitive }?.value?.asString ?: ""
            }
        } catch (ex: Exception) {
            ""
        }
    }

    /**
     * Atualiza os produtos usando a API de sessão POS
     * Agora também tenta buscar categorias via /menu/categories/pos e usa nomes quando disponíveis.
     */
    suspend fun refreshProducts(posAccessToken: String, proxyCredentials: String, menuId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag("ProductsRefresh").d("Fetching products from POS session for menu=$menuId")

                val response = apiRepository.getPosSessionProducts(menuId, posAccessToken)

                if (response.isSuccessful && response.body() != null) {
                    val productsResponse = response.body()!!

                    // 1) Limpa produtos e categorias antigas
                    productDao.clearAll()
                    categoryDao.clearAll()


                    // 2) Tenta obter categorias dedicadas via endpoint /menu/categories/pos
                    val categoriesMap = mutableMapOf<String, String>() // id -> name

                    try {
                        val categoriesResponse = apiRepository.getMenuCategoriesPos(menuId, posAccessToken)

                        if (categoriesResponse.isSuccessful && categoriesResponse.body() != null) {
                            val json = categoriesResponse.body()!!

                            // Caso a API retorne { "categories": [ { id, label, ... }, ... ] }
                            if (json.isJsonObject && json.asJsonObject.has("categories")) {
                                val catsArray = json.asJsonObject.getAsJsonArray("categories")
                                for (el in catsArray) {
                                    try {
                                        val obj = el.asJsonObject
                                        val id = if (obj.has("id")) obj.get("id").asString
                                        else if (obj.has("_id")) obj.get("_id").asString
                                        else ""
                                        val label = when {
                                            obj.has("label") -> obj.get("label").asString
                                            obj.has("name") -> obj.get("name").asString
                                            else -> ""
                                        }
                                        if (id.isNotBlank()) {
                                            categoriesMap[id] = if (label.isNotBlank()) label else "Categoria ${id.take(8)}"
                                        }
                                    } catch (ie: Exception) {
                                        // ignora item inválido
                                    }
                                }
                            } else if (json.isJsonObject && json.asJsonObject.has("products")) {
                                // fallback: endpoint devolveu produtos (como em outros endpoints)
                                val prods = json.asJsonObject.getAsJsonArray("products")
                                for (p in prods) {
                                    try {
                                        val pObj = p.asJsonObject
                                        val catEl = pObj.get("category")
                                        val catId = when {
                                            catEl.isJsonPrimitive -> catEl.asString
                                            catEl.isJsonObject && catEl.asJsonObject.has("id") -> catEl.asJsonObject.get("id").asString
                                            else -> catEl.toString()
                                        }
                                        if (catId.isNotBlank() && !categoriesMap.containsKey(catId)) {
                                            categoriesMap[catId] = "Categoria ${catId.take(8)}"
                                        }
                                    } catch (ie: Exception) { }
                                }
                            } else {
                                Timber.tag("ProductsRefresh").w("categories endpoint returned unexpected JSON, using fallback")
                            }
                        } else {
                            Timber.tag("ProductsRefresh").w("categories endpoint HTTP=${categoriesResponse.code()}, using fallback")
                        }
                    } catch (ex: Exception) {
                        Timber.tag("ProductsRefresh").w("Erro ao buscar categorias dedicadas, usando fallback: ${ex.message}")
                    }

                    val normalizedCategoriesMap = mutableMapOf<String, String>()
                    categoriesMap.forEach { (rawId, rawName) ->
                        val id = rawId.trim()
                        if (id.isBlank()) {
                            // usa id fixo para sem categoria (evita chave vazia no DB)
                            normalizedCategoriesMap["uncategorized"] = rawName.ifBlank { "Uncategorized" }
                        } else {
                            normalizedCategoriesMap[id] = rawName.trim().ifBlank { "Categoria ${id.take(8)}" }
                        }
                    }

                    if (normalizedCategoriesMap.isEmpty()) {
                        val productsByCategoryFallback = productsResponse.products.groupBy { extractIdFromJson(it.category).trim() }
                        productsByCategoryFallback.keys.forEachIndexed { index, categoryIdRaw ->
                            val categoryId = categoryIdRaw.trim().ifBlank { "uncategorized" }
                            val displayName = if (categoryId == "uncategorized") "Uncategorized" else "Categoria ${index + 1}"
                            normalizedCategoriesMap[categoryId] = displayName
                        }
                    }

// Fallback: se ainda está vazio, extrai das productsResponse normalmente
                    if (categoriesMap.isEmpty()) {
                        val productsByCategoryFallback = productsResponse.products.groupBy { extractIdFromJson(it.category) }
                        productsByCategoryFallback.keys.forEachIndexed { index, categoryId ->
                            val displayName = if (categoryId.isBlank()) "Sem categoria" else "Categoria ${index + 1}"
                            categoriesMap[categoryId] = displayName
                        }
                    }

// 3) Inserir categorias normalizadas
                    val categories = normalizedCategoriesMap.map { (id, name) ->
                        CategoryEntity(id = id, name = name)
                    }
                    if (categories.isNotEmpty()) {
                        categoryDao.insertMany(categories)
                    }
                    Timber.tag("ProductsRefresh").d("Categories inserted: ${categories.size} -> ${categories.map { it.id + ":" + it.name }}")

// 4) Inserir produtos normalizando categoryId
                    val products = productsResponse.products.map { p ->
                        val rawCatId = extractIdFromJson(p.category).trim()
                        val categoryId = if (rawCatId.isBlank()) "uncategorized" else rawCatId
                        ProductEntity(
                            id = p.id,
                            name = p.label,
                            thumbnail = p.thumbnail.id,
                            url = p.thumbnail.id,
                            categoryId = categoryId,
                            price = p.price.toLong(),
                            stock = Int.MAX_VALUE,
                            isEnabled = true
                        )
                    }
                    if (products.isNotEmpty()) productDao.insertMany(products)
                    true
                } else {
                    Timber.tag("ProductsRefresh").e("Failed to fetch products: HTTP ${response.code()} body=${response.errorBody()?.string()}")
                    false
                }
            } catch (e: Exception) {
                Timber.tag("ProductsRefresh").e(e, "Error refreshing products: ${e.message}")
                false
            }
        }
    }

    suspend fun getSelectedEventId(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val events = eventDao.getAllEvents()
                val selectedEvent = events.find { it.isSelected }
                selectedEvent?.id
            } catch (e: Exception) {
                Timber.tag("ProductsRefresh").e("Error getting selected event: ${e.message}")
                null
            }
        }
    }

    suspend fun getEventId(sessionPref: SharedPreferences): String? {
        val selectedEventId = getSelectedEventId()
        if (selectedEventId != null) {
            return selectedEventId
        }
        return getEventIdFromPrefs(sessionPref).takeIf { it.isNotEmpty() }
    }

    fun getEventIdFromPrefs(sessionPref: SharedPreferences): String {
        return sessionPref.getString("selected_menu_id", "") ?: ""
    }

    fun getAuthTokenFromPrefs(userPref: SharedPreferences): String {
        return userPref.getString("auth_token", "") ?: ""
    }

    fun getPosAccessTokenFromPrefs(sessionPref: SharedPreferences): String {
        // Ajuste a chave conforme sua implementação real
        return sessionPref.getString("pos_access_token", "") ?: ""
    }

    fun getProxyCredentialsFromPrefs(userPref: SharedPreferences): String {
        // Ajuste a chave conforme sua implementação real
        return userPref.getString("proxy_credentials", "") ?: ""
    }

    /**
     * Atualiza produtos + (Opcional) Download das thumbnails em background
     */
    suspend fun refreshProductsWithSelectedEvent(
        userPref: SharedPreferences,
        sessionPref: SharedPreferences
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Tokens necessários para chamar as APIs
                val posAccessToken = getPosAccessTokenFromPrefs(sessionPref)
                val proxyCredentials = getProxyCredentialsFromPrefs(userPref)

                if (posAccessToken.isEmpty()) {
                    Timber.tag("ProductsRefresh").e("No POS access token found")
                    return@withContext false
                }

                if (proxyCredentials.isEmpty()) {
                    Timber.tag("ProductsRefresh").e("No proxy credentials found")
                    return@withContext false
                }

                // 2) Obtém o menuId (event/menu selecionado)
                val menuId = getEventId(sessionPref)
                if (menuId.isNullOrEmpty()) {
                    Timber.tag("ProductsRefresh")
                        .e("No menuId/eventId found, cannot download thumbnails")
                    // Não falha o processo de produtos; só não baixa thumbnails
                    return@withContext true
                }

                // 1) Atualiza produtos da sessão POS (agora também busca categorias)
                Timber.tag("ProductsRefresh").d("Refreshing products with POS session")
                val productsOk = refreshProducts(posAccessToken, proxyCredentials, menuId)

                if (!productsOk) {
                    Timber.tag("ProductsRefresh")
                        .e("Product refresh failed, skipping thumbnails download")
                    return@withContext false
                }

                // 7) (Opcional) Download das thumbnails em background
                try {
                    Timber.tag("ProductsRefresh")
                        .d("Starting background download of thumbnails for menuId=$menuId")

                    val thumbnailsFile = apiRepository.downloadAllProductThumbnails(
                        menuId = menuId,
                        posAccessToken = posAccessToken,
                        proxyCredentials = proxyCredentials
                    )

                    if (thumbnailsFile != null) {
                        Timber.tag("ProductsRefresh")
                            .d("Thumbnails downloaded successfully: ${thumbnailsFile.absolutePath}")
                    } else {
                        Timber.tag("ProductsRefresh")
                            .e("Thumbnails download returned null file for menuId=$menuId")
                    }
                } catch (e: Exception) {
                    Timber.tag("ProductsRefresh")
                        .e(e, "Error downloading thumbnails for menuId=$menuId: ${e.message}")
                }

                true
            } catch (e: Exception) {
                Timber.tag("ProductsRefresh").e(e, "Error in refreshProductsWithSelectedEvent: ${e.message}")
                false
            }
        }
    }

    suspend fun getSelectedEvent(): EventEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val events = eventDao.getAllEvents()
                events.find { it.isSelected }
            } catch (e: Exception) {
                null
            }
        }
    }
}