// LoginConfirmViewModel.kt
package br.com.ticpass.pos.viewmodel.login

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.api.GetEventProductsResponse
import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.dao.PosDao
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.entity.PosEntity
import br.com.ticpass.pos.data.room.repository.PosRepository
import br.com.ticpass.pos.data.room.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

import javax.inject.Inject
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.room.dao.CategoryDao
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.CategoryEntity
import br.com.ticpass.pos.data.room.entity.ProductEntity
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.data.room.repository.ProductRepository
import androidx.core.content.edit
import br.com.ticpass.pos.data.room.dao.CashierDao
import br.com.ticpass.pos.data.room.entity.CashierEntity
import br.com.ticpass.pos.data.room.repository.CashierRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@HiltViewModel
class LoginConfirmViewModel @Inject constructor(
    private val posDao: PosDao,
    private val eventDao: EventDao,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val cashierDao: CashierDao,
    private val apiRepository: APIRepository
) : ViewModel() {

    suspend fun confirmLogin(sessionPref: SharedPreferences, userPref: SharedPreferences) {
        withContext(Dispatchers.IO) {
            try {
                clearPreviousSelections()
                val posEntity = createPosEntity(sessionPref)
                posDao.upsertPos(posEntity)
                val eventEntity = createEventEntity(sessionPref)
                eventDao.upsertEvent(eventEntity)
                val cashierEntity = createCashierEntity(userPref)
                cashierDao.insertUser(cashierEntity)
                fetchAndInsertProducts(sessionPref, userPref, eventEntity.id)
                sessionPref.edit { clear() }
            } catch (e: Exception) {
                Log.e("LoginConfirmVM", "Erro durante confirmação de login", e)
                throw e
            }
        }
    }

    private suspend fun clearPreviousSelections() {
        posDao.deselectAllPos()
        eventDao.deselectAllEvents()
    }

    private fun createPosEntity(sessionPref: SharedPreferences): PosEntity {
        return PosEntity(
            id = sessionPref.getString("pos_id", "")!!,
            name = sessionPref.getString("pos_name", "")!!,
            cashier = "", // Atualizado depois com cashier real
            commission = sessionPref.getLong("pos_commission", 0L),
            isClosed = false,
            isSelected = true
        )
    }

    private fun createEventEntity(sessionPref: SharedPreferences): EventEntity {
        return EventEntity(
            id = sessionPref.getString("selected_menu_id", "")!!,
            name = sessionPref.getString("selected_menu_name", "")!!,
            dateStart = sessionPref.getString("selected_menu_dateStart", "")!!,
            dateEnd = sessionPref.getString("selected_menu_dateEnd", "")!!,
            logo = sessionPref.getString("selected_menu_logo", "")!!,
            pin = sessionPref.getString("selected_menu_pin", "")!!,
            details = sessionPref.getString("selected_menu_details", "")!!,
            mode = sessionPref.getString("selected_menu_mode", "")!!,
            isSelected = true, // Garante que este evento está selecionado
            printingPriceEnabled = false,
            ticketsPrintingGrouped = false,
            hasProducts = false,
            isCreditEnabled = false,
            isDebitEnabled = false,
            isPIXEnabled = false,
            ticketFormat = "default",
            isVREnabled = false,
            isLnBTCEnabled = false,
            isCashEnabled = false,
            isAcquirerPaymentEnabled = false,
            isMultiPaymentEnabled = false
        )
    }

    private fun createCashierEntity(userPref: SharedPreferences): CashierEntity {
        val userId = when (val value = userPref.all["user_id"]) {
            is String -> value
            is Int -> value.toString()
            else -> ""
        }
        return CashierEntity(
            id = userId,
            name = userPref.getString("user_name", "") ?: ""
        )
    }

    suspend fun fetchAndInsertProducts(
        sessionPref: SharedPreferences,
        userPref: SharedPreferences,
        eventId: String
    ) {
        val jwt = userPref.getString("auth_token", "")!!
        val response = apiRepository.getEventProducts(event = eventId, jwt = jwt)

        if (response.status == 200) {
            // Inserir categorias
            val categories = response.result.map { cat ->
                CategoryEntity(id = cat.id, name = cat.name)
            }
            categoryDao.insertMany(categories)

            // Inserir produtos
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
        }
    }
}