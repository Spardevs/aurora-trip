package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.products.ProductsListScreen

@AndroidEntryPoint
class ProductsActivity : DrawerBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, ProductsListScreen())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // TODO: ação de busca
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun openProducts() {
        startActivity(Intent(this, ProductsActivity::class.java))

    }

    override fun openProfile() {
//        startActivity(Intent(this, ProfileActivity::class.java))
    }
}
