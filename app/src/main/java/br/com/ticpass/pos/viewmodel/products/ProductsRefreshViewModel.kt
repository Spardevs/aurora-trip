// ProductsRefreshViewModel.kt
package br.com.ticpass.pos.viewmodel.products

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.room.dao.CategoryDao
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
    private val categoryDao: CategoryDao
) : ViewModel() {

    suspend fun refreshProducts(eventId: String, jwt: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiRepository.getEventProducts(event = eventId, jwt = jwt)

                if (response.status == 200) {
                    productDao.clearAll()
                    categoryDao.clearAll()

                    val categories = response.result.map { cat ->
                        CategoryEntity(id = cat.id, name = cat.name)
                    }
                    categoryDao.insertMany(categories)

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
                    productDao.insertMany(products)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun getEventIdFromPrefs(sessionPref: SharedPreferences): String {
        return sessionPref.getString("selected_menu_id", "") ?: ""
    }

    fun getAuthTokenFromPrefs(userPref: SharedPreferences): String {
        return userPref.getString("auth_token", "") ?: ""
    }
}