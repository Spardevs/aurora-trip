// ProductsRefreshViewModel.kt
package br.com.ticpass.pos.viewmodel.products

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.room.dao.CategoryDao
import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.CategoryEntity
import br.com.ticpass.pos.data.room.entity.ProductEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProductsRefreshViewModel @Inject constructor(
    private val apiRepository: APIRepository,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val eventDao: EventDao
) : ViewModel() {

    suspend fun refreshProducts(eventId: String, jwt: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ProductsRefreshVM", "=== INICIANDO REFRESH ===")
                Log.d("ProductsRefreshVM", "Event ID recebido: $eventId")
                Log.d("ProductsRefreshVM", "JWT recebido: ${jwt.take(20)}...")

                // Verificar se o evento existe no banco local
                val event = eventDao.getEventById(eventId)
                Log.d("ProductsRefreshVM", "Evento encontrado no BD: ${event != null}")

                Log.d("ProductsRefreshVM", "Chamando API...")
                val response = apiRepository.getEventProducts(event = eventId, jwt = jwt)

                Log.d("ProductsRefreshVM", "Resposta da API - Status: ${response.status}")
                Log.d("ProductsRefreshVM", "Número de categorias: ${response.result.size}")

                if (response.status == 200) {
                    Log.d("ProductsRefreshVM", "Limpando tabelas...")
                    val deletedProducts = productDao.clearAll()
                    val deletedCategories = categoryDao.clearAll()
                    Log.d("ProductsRefreshVM", "Produtos removidos: $deletedProducts")
                    Log.d("ProductsRefreshVM", "Categorias removidas: $deletedCategories")

                    val categories = response.result.map { cat ->
                        CategoryEntity(id = cat.id, name = cat.name)
                    }
                    Log.d("ProductsRefreshVM", "Categorias para inserir: ${categories}")

                    val insertedCategories = categoryDao.insertMany(categories)
                    Log.d("ProductsRefreshVM", "Categorias inseridas: ${insertedCategories}")

                    val products = response.result.flatMap { cat ->
                        cat.products.map { p ->
                            ProductEntity(
                                id = p.id,
                                name = p.title,
                                thumbnail = p.photo,
                                url = p.photo,
                                categoryId = cat.id,
                                price = p.value.toLong(),
                                stock = p.stock.toInt(),
                                isEnabled = true
                            )
                        }
                    }
                    Log.d("ProductsRefreshVM", "Produtos para inserir: ${products.size}")

                    val insertedProducts = productDao.insertMany(products)
                    Log.d("ProductsRefreshVM", "Produtos inseridos: ${insertedProducts}")

                    Log.d("ProductsRefreshVM", "=== REFRESH CONCLUÍDO COM SUCESSO ===")
                    true
                } else {
                    Log.d("ProductsRefreshVM", "=== ERRO NA RESPOSTA DA API: ${response.status} ===")
                    false
                }
            } catch (e: Exception) {
                Log.e("ProductsRefreshVM", "=== ERRO EXCEÇÃO ===", e)
                false
            }
        }
    }

    fun getEventIdFromPrefs(sessionPref: SharedPreferences): String {
        val allPrefs = sessionPref.all
        Log.d("ProductsRefreshVM", "Todas as session prefs: $allPrefs")

        val eventId = sessionPref.getString("selected_menu_id", "") ?: ""
        Log.d("ProductsRefreshVM", "Event ID das session prefs: '$eventId'")
        return eventId
    }

    fun getAuthTokenFromPrefs(userPref: SharedPreferences): String {
        val allPrefs = userPref.all
        Log.d("ProductsRefreshVM", "Todas as user prefs: $allPrefs")

        val authToken = userPref.getString("auth_token", "") ?: ""
        Log.d("ProductsRefreshVM", "Auth Token das user prefs: '${authToken.take(20)}...'")
        return authToken
    }
}