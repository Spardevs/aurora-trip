package br.com.ticpass.pos.view.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.BaseActivity
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.api.PosItem
import br.com.ticpass.pos.data.model.Menu
import br.com.ticpass.pos.view.ui.login.adapter.PosAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PosScreen : BaseActivity() {

    @Inject lateinit var apiRepository: APIRepository

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

        val menuId = intent.getStringExtra(EXTRA_MENU_ID)
            ?: throw IllegalArgumentException("menuId não foi passado na Intent")

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

        val jwt = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("auth_token", "")!!

        lifecycleScope.launch {
            val response = apiRepository.getPosList(
                event = menuId,
                jwt = jwt,
            )
            if (response.status == 200) {
                adapter.setItems(response.result.items)
            } else {
                Toast.makeText(this@PosScreen,
                    "Erro ${response.status}: ${response.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }
}