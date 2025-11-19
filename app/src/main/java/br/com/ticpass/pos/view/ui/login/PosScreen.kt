package br.com.ticpass.pos.view.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.data.api.ApiRepository
import br.com.ticpass.pos.data.model.Pos
import br.com.ticpass.pos.view.ui.login.adapter.PosAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.inject.Inject
import com.google.gson.Gson
import com.google.gson.JsonParser

@AndroidEntryPoint
class PosScreen : BaseActivity() {

    @Inject lateinit var apiRepository: ApiRepository

    companion object {
        private const val EXTRA_MENU_ID = "extra_menu_id"
        fun newIntent(context: Context, menuId: String): Intent {
            return Intent(context, PosScreen::class.java).apply {
                putExtra(EXTRA_MENU_ID, menuId)
            }
        }
    }

    @SuppressLint("UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)

        // ✅ Pega o menuId do SharedPreferences (salvo pelo MenuActivity)
        val prefs = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val finalMenuId = prefs.getString("selected_menu_id", null)
            ?: throw IllegalArgumentException("selected_menu_id não encontrado em SessionPrefs")

        val recycler = findViewById<RecyclerView>(R.id.pos_recycler_view)
        recycler.layoutManager = GridLayoutManager(this, 3)

        val adapter = PosAdapter(onClick = { item ->
            val sharedPref = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("pos_id", item.id)
                putString("pos_name", item.name)
                putLong("pos_commission", item.commission?.toLong() ?: 0)
                apply()
            }

            val intent = ConfirmScreen.newIntent(this)
            startActivity(intent)
        })
        recycler.adapter = adapter

        // ✅ Chama a API v2 para buscar os POS do menu
        lifecycleScope.launch {
            try {
                Log.d("PosScreen", "Buscando POS para menu: $finalMenuId")

                val resp = apiRepository.getMenuPos(
                    take = 10,
                    page = 1,
                    menu = finalMenuId,
                    available = "both"
                )

                if (!resp.isSuccessful) {
                    val errorMsg = resp.errorBody()?.string() ?: "Erro desconhecido"
                    Toast.makeText(
                        this@PosScreen,
                        "Falha ao carregar POS (HTTP ${resp.code()}): $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("PosScreen", "Erro HTTP ${resp.code()}: $errorMsg")
                    return@launch
                }

                val body = resp.body() ?: run {
                    Toast.makeText(
                        this@PosScreen,
                        "Resposta vazia ao carregar POS",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Log.d("PosScreen", "POS carregados: ${body.edges.size} itens")

                // ✅ Mapear PosEdge (API v2) -> PosItem (modelo da UI)
                val items = body.edges.map { edge ->

                    fun extractCashierName(raw: Any?): String? {
                        if (raw == null) return null
                        try {
                            // Log para debugar o que está chegando
                            Log.d("PosScreen", "raw cashier (toString): ${raw.toString()} (class=${raw::class.java})")

                            // Serializa qualquer objeto para JSON e parseia com Gson/JsonParser
                            val gson = Gson()
                            val json = gson.toJson(raw)
                            Log.d("PosScreen", "raw cashier asJson: $json")

                            val el = JsonParser.parseString(json)
                            if (el.isJsonObject) {
                                val obj = el.asJsonObject
                                when {
                                    obj.has("name") && !obj.get("name").isJsonNull -> return obj.get("name").asString
                                    obj.has("nome") && !obj.get("nome").isJsonNull -> return obj.get("nome").asString
                                    obj.has("username") && !obj.get("username").isJsonNull -> return obj.get("username").asString
                                    obj.has("email") && !obj.get("email").isJsonNull -> return obj.get("email").asString
                                    else -> {
                                        // se não achou campos previstos, tenta devolver um campo legível
                                        // por exemplo, procura a primeira string não-nula nas propriedades
                                        obj.entrySet().forEach { entry ->
                                            val v = entry.value
                                            if (v.isJsonPrimitive) {
                                                val s = v.asString
                                                if (!s.isNullOrBlank()) return s
                                            }
                                        }
                                        return json
                                    }
                                }
                            }
                            if (el.isJsonPrimitive) {
                                return el.asString
                            }

                            // Fallbacks: tenta extrair do toString com regex (data class / Map)
                            val s = raw.toString()
                            if (s.contains("name=")) {
                                val regex = Regex("name=([^,\\)]+)")
                                regex.find(s)?.groups?.get(1)?.value?.trim()?.let { return it }
                            }
                            if (s.contains("nome=")) {
                                val regex = Regex("nome=([^,\\)]+)")
                                regex.find(s)?.groups?.get(1)?.value?.trim()?.let { return it }
                            }
                            if (s.contains("username=")) {
                                val regex = Regex("username=([^,\\)]+)")
                                regex.find(s)?.groups?.get(1)?.value?.trim()?.let { return it }
                            }

                            // último recurso
                            return s
                        } catch (ex: Exception) {
                            Log.e("PosScreen", "Erro ao extrair cashier name", ex)
                            return raw.toString()
                        }
                    }
                    Pos(
                        id = edge.id,
                        name = "${edge.prefix} ${edge.sequence}",
                        commission = BigInteger.valueOf(edge.commission.toLong()),
                        session = if (edge.session == null) {
                            null
                        } else {
                            Pos.PosSessionUI(
                                cashier = extractCashierName(edge.session.cashier)
                            )
                        }
                    )
                }

                adapter.setItems(items)

            } catch (e: Exception) {
                Log.e("PosScreen", "Erro ao carregar POS", e)
                Toast.makeText(
                    this@PosScreen,
                    "Erro ao carregar POS: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}