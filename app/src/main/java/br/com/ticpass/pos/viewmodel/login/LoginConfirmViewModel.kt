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
    private val cashierDao: CashierDao
) : ViewModel() {
    @Inject lateinit var apiRepository: APIRepository

    suspend fun insertInfo(sessionPref: SharedPreferences, userPref: SharedPreferences) {
        withContext(Dispatchers.IO) {
            val posEntity = PosEntity(
                id         = sessionPref.getString("pos_id","")!!,
                name       = sessionPref.getString("pos_name","")!!,
                cashier    = "tteste",
                commission = sessionPref.getLong("pos_commission",0L),
                isClosed   = false,
                isSelected = true
            )
            posDao.upsertPos(posEntity)
            val menuEntity = EventEntity(
                id          = sessionPref.getString("selected_menu_id","")!!,
                name        = sessionPref.getString("selected_menu_name","")!!,
                dateStart   = sessionPref.getString("selected_menu_dateStart","")!!,
                dateEnd     = sessionPref.getString("selected_menu_dateEnd","")!!,
                logo        = sessionPref.getString("selected_menu_logo","")!!,
                pin         = sessionPref.getString("selected_menu_pin","")!!,
                details     = sessionPref.getString("selected_menu_details","")!!,
                mode        = sessionPref.getString("selected_menu_mode","")!!,
                isSelected  = sessionPref.getBoolean("selected_menu_isSelected",false),
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
            eventDao.upsertEvent(menuEntity)

            val menuId = menuEntity.id
            val jwt    = userPref.getString("auth_token","")!!
            val resp   = apiRepository.getEventProducts(event = menuId, jwt = jwt)
            if (resp.status == 200) {
                val cats = resp.result.map { c -> CategoryEntity(id = c.id, name = c.name) }
                categoryDao.insertMany(cats)
                val prods = resp.result.flatMap { c ->
                    c.products.map { p ->
                        ProductEntity(
                            id         = p.id,
                            name       = p.title,
                            thumbnail  = p.photo,
                            url        = p.photo,
                            categoryId = c.id,
                            price      = p.value.toLong(),
                            stock      = p.stock.toInt(),
                            isEnabled  = true
                        )
                    }
                }
                productDao.insertMany(prods)
            }
            sessionPref.edit { clear() }
        }

        insertPosInfo(sessionPref)
        insertMenuInfo(sessionPref)
        insertProductsInfo(sessionPref, userPref)
        insertCashierInfo(userPref)
        cleanSessionPrefs(sessionPref)
    }

    fun insertPosInfo(sessionPref: SharedPreferences) {
        val posRepo = PosRepository(posDao)

        val posEntity = PosEntity(
            id         = sessionPref.getString("pos_id", "")!!,
            name       = sessionPref.getString("pos_name", "")!!,
            cashier    = "tteste",
            commission = sessionPref.getLong("pos_commission", 0L),
            isClosed   = false,
            isSelected = true,
        )

        viewModelScope.launch {
            try {
                Log.d("posEntity", "$posEntity")
                posRepo.upsertPos(posEntity)
            } catch (e: Exception) {
                Log.e("LoginConfirmVM", "Erro ao atualizar POS", e)
            }
        }
    }

    fun insertCashierInfo(userPref: SharedPreferences) {
        val cashierRepo = CashierRepository(cashierDao)

        val userId = when (val value = userPref.all["user_id"]) {
            is String -> value
            is Int -> value.toString()
            else -> ""
        }

        val userName = userPref.getString("user_name", "") ?: ""

        val cashierEntity = CashierEntity(
            id = userId,
            name = userName
        )

        viewModelScope.launch {
            try {
                cashierRepo.insertUser(cashierEntity)
            } catch (e: Exception) {
                Log.e("LoginConfirmVM", "Erro ao atualizar Cashier", e)
            }
        }
    }

    private fun insertMenuInfo(sessionPref: SharedPreferences ) {
        val eventRepo = EventRepository(eventDao)

        val menuEntity = EventEntity(
            id = sessionPref.getString("selected_menu_id", "")!!,
            name = sessionPref.getString("selected_menu_name", "")!!,
            dateStart = sessionPref.getString("selected_menu_dateStart", "")!!,
            dateEnd = sessionPref.getString("selected_menu_dateEnd", "")!!,
            logo = sessionPref.getString("selected_menu_logo", "")!!,
            pin = sessionPref.getString("selected_menu_pin", "")!!,
            details = sessionPref.getString("selected_menu_details", "")!!,
            mode = sessionPref.getString("selected_menu_mode", "")!!,
            isSelected = sessionPref.getBoolean("selected_menu_isSelected", false),
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

        viewModelScope.launch {
            try {
                eventRepo.upsertEvent(menuEntity)
            } catch (e: Exception) {
                Log.e("LoginConfirmVM", "Erro ao atualizar evento", e)
            }
        }
    }

    fun insertProductsInfo(sessionPref: SharedPreferences, userPref: SharedPreferences) {
        try {
            val menuId = sessionPref.getString("selected_menu_id", "")!!
            val jwt    = userPref.getString("auth_token", "")!!
            viewModelScope.launch {
                val resp = fetchProducts(menuId, jwt)
                if (resp.status == 200) {
                    val categoryEntities = resp.result.map { cat ->
                        CategoryEntity(
                            id = cat.id,
                            name = cat.name
                        )
                    }
                    val categoryRepo = CategoryRepository(categoryDao)
                    categoryRepo.insertMany(categoryEntities)

                    val productEntities = resp.result.flatMap { cat ->
                        cat.products.map { p ->
                            ProductEntity(
                                id         = p.id,
                                name       = p.title,
                                thumbnail  = p.photo,
                                url        = p.photo,
                                categoryId   = cat.id,
                                price      = p.value.toLong(),
                                stock      = p.stock.toInt(),
                                isEnabled  = true
                            )
                        }
                    }
                    val productRepo = ProductRepository(productDao)
                    productRepo.insertMany(productEntities)
                }
            }
        } catch (e: Exception) {
            Log.e("LoginConfirmVM", "Erro ao atualizar produtos", e)
        }
    }

    private suspend fun fetchProducts(menuId: String, jwt: String): GetEventProductsResponse {
        val resp =  apiRepository.getEventProducts(event = menuId, jwt = jwt)
        Log.d("LoginConfirmVM", "fetchProducts: $resp")
        return resp
    }

    private fun cleanSessionPrefs(sessionPref: SharedPreferences) {
        sessionPref.edit {
            clear()
        }
    }
}