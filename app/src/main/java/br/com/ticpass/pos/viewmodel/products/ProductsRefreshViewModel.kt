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
import br.com.ticpass.pos.data.room.entity.EventEntity
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
                val events = eventDao.getAllEvents()
                val event = events.find { it.id == eventId }

                if (event == null) {
                    Log.e("ProductsRefresh", "Event not found with ID: $eventId")
                    return@withContext false
                }

                val response = apiRepository.getEventProducts(event = eventId, jwt = jwt)
                if (response.status == 200) {
                    val deletedProducts = productDao.clearAll()
                    val deletedCategories = categoryDao.clearAll()

                    val categories = response.result.map { cat ->
                        CategoryEntity(id = cat.id, name = cat.name)
                    }

                    val insertedCategories = categoryDao.insertMany(categories)

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
                    val insertedProducts = productDao.insertMany(products)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("ProductsRefresh", "Error refreshing products: ${e.message}")
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
                Log.e("ProductsRefresh", "Error getting selected event: ${e.message}")
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

    suspend fun refreshProductsWithSelectedEvent(
        userPref: SharedPreferences,
        sessionPref: SharedPreferences
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val eventId = getSelectedEventId()
                val jwt = getAuthTokenFromPrefs(userPref)

                if (eventId == null) {
                    Log.e("ProductsRefresh", "No event ID found in database")
                    return@withContext false
                }

                if (jwt.isEmpty()) {
                    Log.e("ProductsRefresh", "No auth token found")
                    return@withContext false
                }

                Log.d("ProductsRefresh", "Refreshing products for event: $eventId")
                return@withContext refreshProducts(eventId, jwt)
            } catch (e: Exception) {
                Log.e("ProductsRefresh", "Error in refreshProductsWithSelectedEvent: ${e.message}")
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