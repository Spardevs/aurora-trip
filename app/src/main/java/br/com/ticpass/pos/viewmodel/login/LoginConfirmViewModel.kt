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
import com.google.gson.JsonElement
import timber.log.Timber

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

    // helper: extrai uma string "id" / valor legível de JsonElement? (category/menu etc.)
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
                obj.entrySet().firstOrNull()?.value?.isJsonPrimitive == true -> obj.entrySet().first().value.asString
                else -> obj.toString()
            }
        } catch (ex: Exception) {
            el.toString()
        }
    }

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
                else -> {
                    // tenta pegar o primeiro campo string legível
                    obj.entrySet().firstOrNull { it.value.isJsonPrimitive }?.value?.asString ?: ""
                }
            }
        } catch (ex: Exception) {
            ""
        }
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
        val productsResponse = apiRepository.getPosSessionProducts(menuId, posAccessToken)

        if (!productsResponse.isSuccessful || productsResponse.body() == null) {
            Log.e("LoginConfirmVM", "Falha ao buscar produtos da sessão POS: code=${productsResponse.code()}")
            return
        }

        val productsFromApi = productsResponse.body()!!.products
        Log.d("LoginConfirmVM", "Produtos recebidos: ${productsFromApi.size}")

// 3.5) Tentar buscar categorias dedicadas via endpoint /menu/categories/pos
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

// 4) Inserir categorias no DB com nomes corretos
        val categories = categoriesMap.map { (id, name) ->
            CategoryEntity(id = id, name = name)
        }
        if (categories.isNotEmpty()) categoryDao.insertMany(categories)
        Log.d("LoginConfirmVM", "Categorias inseridas: ${categories.size}")

// 6) Inserir produtos (usando categoryId correto)
        val products = productsFromApi.map { p ->
            val categoryId = extractIdFromJson(p.category)
            ProductEntity(
                id = p.id,
                name = p.label,
                thumbnail = p.thumbnail.id, // Salvar ID da thumbnail
                url = p.thumbnail.id,
                categoryId = categoryId,
                price = p.price.toLong(),
                stock = 999,
                isEnabled = true
            )
        }
        if (products.isNotEmpty()) {
            productDao.insertMany(products)
        }
        Log.d("LoginConfirmVM", "Produtos inseridos: ${products.size}")

        // 7) (Opcional) Download das thumbnails em background
        // Pode ser implementado posteriormente usando api2Repository.downloadAllProductThumbnails(...)
    }
}