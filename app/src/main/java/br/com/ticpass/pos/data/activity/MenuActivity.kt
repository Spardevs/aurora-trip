package br.com.ticpass.pos.data.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.Menu
import br.com.ticpass.pos.view.ui.login.MenuScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menus)

        val menus = listOf(
            Menu("Evento 1", "Descrição do evento 1", 1),
            Menu("Evento 2", "Descrição do evento 2", 2),
            Menu("Evento 3", "Descrição do evento 3", 3),
            Menu("Evento 4", "Descrição do evento 4", 4)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.menusRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MenuScreen(menus) { event ->
            // Handle event click
        }
    }
}