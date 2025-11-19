// LoginConfirmViewModel.kt
package br.com.ticpass.pos.viewmodel.login

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.dao.PosDao
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.entity.PosEntity
import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject
import br.com.ticpass.pos.data.api.ApiRepository
import br.com.ticpass.pos.data.room.dao.CategoryDao
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.CategoryEntity
import br.com.ticpass.pos.data.room.entity.ProductEntity
import androidx.core.content.edit
import br.com.ticpass.pos.data.room.dao.CashierDao
import br.com.ticpass.pos.data.room.entity.CashierEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@HiltViewModel
class LoginConfirmViewModel @Inject constructor(
    private val posDao: PosDao,
    private val eventDao: EventDao,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val cashierDao: CashierDao,
    private val apiRepository: ApiRepository
) : ViewModel() {

    suspend fun confirmLogin(sessionPref: SharedPreferences, userPref: SharedPreferences, isAlreadyLogged: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                clearPreviousSelections()
                val posEntity = createPosEntity(sessionPref, userPref, isAlreadyLogged)
                posDao.upsertPos(posEntity)
                val eventEntity = createEventEntity(sessionPref)
                eventDao.upsertEvent(eventEntity)

                if (!isAlreadyLogged) {
                    // Primeiro login - cria novo cashier
                    val cashierEntity = createCashierEntity(userPref)
                    cashierDao.insertUser(cashierEntity)
                } else {
                    // Já logado - busca cashier existente do SharedPreferences
                    Log.d("LoginConfirmVM", "Usuário já logado, buscando caixa do SharedPreferences")
                }

                fetchAndInsertProducts(sessionPref, userPref, eventEntity.id)
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

    private fun createPosEntity(sessionPref: SharedPreferences, userPref: SharedPreferences, isAlreadyLogged: Boolean): PosEntity {
        val cashierName = if (isAlreadyLogged) {
            // Busca o nome do operador salvo no SharedPreferences
            userPref.getString("operator_name", "") ?: ""
        } else {
            ""
        }

        return PosEntity(
            id = sessionPref.getString("pos_id", "")!!,
            name = sessionPref.getString("pos_name", "")!!,
            cashier = cashierName,
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
            isMultiPaymentEnabled = false,
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
        // 1) Obter tokens e credenciais
        val posAccessToken = userPref.getString("auth_token", "") ?: ""
        val proxyCredentials = userPref.getString("proxy_credentials", "") ?: ""

        if (posAccessToken.isBlank() || proxyCredentials.isBlank()) {
            Log.e("LoginConfirmVM", "Tokens vazios ao tentar abrir sessão POS")
            return
        }

        val posId = sessionPref.getString("pos_id", "") ?: ""
        val deviceId = sessionPref.getString("device_id", "") ?: ""
        val cashierId = when (val value = userPref.all["user_id"]) {
            is String -> value
            is Int -> value.toString()
            else -> ""
        }

        // 2) Abrir sessão POS
        Log.d("LoginConfirmVM", "Abrindo sessão POS: pos=$posId, device=$deviceId, cashier=$cashierId")
        val openSessionResponse = apiRepository.openPosSession(
            posAccessToken = posAccessToken,
            proxyCredentials = proxyCredentials,
            pos = posId,
            device = deviceId,
            cashier = cashierId
        )

        if (!openSessionResponse.isSuccessful || openSessionResponse.body() == null) {
            Log.e("LoginConfirmVM", "Falha ao abrir sessão POS: code=${openSessionResponse.code()}")
            return
        }

        val posSessionId = openSessionResponse.body()!!.id
        Log.d("LoginConfirmVM", "Sessão POS aberta com sucesso: $posSessionId")

        // Salvar pos_session_id no SharedPreferences
        sessionPref.edit {
            putString("pos_session_id", posSessionId)
        }

         val menuId: String by lazy {
            val value = sessionPref.all["selected_menu_id"]
            when (value) {
                is String -> value
                is Int -> value.toString()
                else -> ""
            }
        }

        // 3) Buscar produtos da sessão
        val productsResponse = apiRepository.getPosSessionProducts(menuId = menuId)

        if (!productsResponse.isSuccessful || productsResponse.body() == null) {
            Log.e("LoginConfirmVM", "Falha ao buscar produtos da sessão POS: code=${productsResponse.code()}")
            return
        }

        val productsFromApi = productsResponse.body()!!.products
        Log.d("LoginConfirmVM", "Produtos recebidos: ${productsFromApi.size}")

        // 4) Agrupar produtos por categoria
        val categoriesMap = mutableMapOf<String, String>()
        productsFromApi.forEach { product ->
            if (!categoriesMap.containsKey(product.category)) {
                // Usar o ID da categoria como nome temporário (pode ser melhorado)
                categoriesMap[product.category] = "Categoria ${product.category.take(8)}"
            }
        }

        // 5) Inserir categorias
        val categories = categoriesMap.map { (id, name) ->
            CategoryEntity(id = id, name = name)
        }
        categoryDao.insertMany(categories)
        Log.d("LoginConfirmVM", "Categorias inseridas: ${categories.size}")

        // 6) Inserir produtos
        val products = productsFromApi.map { p ->
            ProductEntity(
                id = p.id,
                name = p.label,
                thumbnail = p.thumbnail.id, // Salvar ID da thumbnail
                url = p.thumbnail.id,       // Pode ser usado para download posterior
                categoryId = p.category,
                price = p.price.toLong(),
                stock = 999,                // API v2 não retorna stock, usar valor padrão
                isEnabled = true
            )
        }
        productDao.insertMany(products)
        Log.d("LoginConfirmVM", "Produtos inseridos: ${products.size}")

        // 7) (Opcional) Download das thumbnails em background
        // Pode ser implementado posteriormente usando api2Repository.downloadAllProductThumbnails(...)
    }
}
