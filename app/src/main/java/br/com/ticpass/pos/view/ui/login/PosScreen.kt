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
                    Pos(
                        id = edge.id,
                        name = "${edge.prefix} ${edge.sequence}",
                        commission = BigInteger.valueOf(edge.commission.toLong()),
                        session = if (edge.session == null) {
                            null
                        } else {
                            Pos.PosSessionUI(
                                cashier = edge.session.cashier.name
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