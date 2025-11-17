package br.com.ticpass.pos.viewmodel.products

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

@HiltViewModel
class ProductsRefreshViewModel @Inject constructor(
    private val apiRepository: ApiRepository,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val eventDao: EventDao
) : ViewModel() {

    /**
     * Atualiza os produtos usando a API de sessão POS
     */
    suspend fun refreshProducts(posAccessToken: String, proxyCredentials: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag("ProductsRefresh").d("Fetching products from POS session")

                val response = apiRepository.getPosSessionProducts(
                    posAccessToken = posAccessToken,
                    proxyCredentials = proxyCredentials
                )

                if (response.isSuccessful && response.body() != null) {
                    val productsResponse = response.body()!!

                    // 1) Limpa produtos e categorias antigas
                    productDao.clearAll()
                    categoryDao.clearAll()

                    // 2) Agrupa produtos por categoria
                    val productsByCategory = productsResponse.products.groupBy { it.category }

                    // 3) Cria as categorias (nome provisório se a API não enviar o nome)
                    val categories = productsByCategory.keys.mapIndexed { index, categoryId ->
                        CategoryEntity(
                            id = categoryId,
                            name = "Categoria ${index + 1}" // Ajuste se tiver nome real de categoria
                        )
                    }
                    categoryDao.insertMany(categories)

                    // 4) Cria os produtos
                    val products = productsResponse.products.map { p ->
                        ProductEntity(
                            id = p.id,
                            name = p.label,
                            thumbnail = p.thumbnail.id, // ID da thumbnail
                            url = p.thumbnail.id,
                            categoryId = p.category,
                            price = p.price.toLong(),
                            stock = Int.MAX_VALUE, // Sem info de estoque na nova API
                            isEnabled = true
                        )
                    }
                    productDao.insertMany(products)

                    Timber.tag("ProductsRefresh")
                        .d("Inserted ${categories.size} categories and ${products.size} products")

                    true
                } else {
                    Timber.tag("ProductsRefresh").e("Failed to fetch products: HTTP ${response.code()} body=${
                        response.errorBody()?.string()
                    }"
                    )
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

                // 1) Atualiza produtos da sessão POS
                Timber.tag("ProductsRefresh").d("Refreshing products with POS session")
                val productsOk = refreshProducts(posAccessToken, proxyCredentials)

                if (!productsOk) {
                    Timber.tag("ProductsRefresh")
                        .e("Product refresh failed, skipping thumbnails download")
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

                // 7) (Opcional) Download das thumbnails em background
                // Usa apiRepository.downloadAllProductThumbnails(...)
                try {
                    Timber.tag("ProductsRefresh")
                        .d("Starting background download of thumbnails for menuId=$menuId")

                    // Aqui está a chamada pedida
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
                Timber.tag("ProductsRefresh")
                    .e(e, "Error in refreshProductsWithSelectedEvent: ${e.message}")
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