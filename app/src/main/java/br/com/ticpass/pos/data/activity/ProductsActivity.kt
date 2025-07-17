package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.login.LoginScreen
import br.com.ticpass.pos.view.ui.products.ProductsListScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.content.edit

@AndroidEntryPoint
class ProductsActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView      = findViewById(R.id.nav_view)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val footer = layoutInflater
            .inflate(R.layout.nav_drawer_footer, navView, false)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        navView.addView(footer)

        footer.setOnClickListener {
            getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .edit { clear() }
            startActivity(Intent(this, LoginScreen::class.java))
            finish()
        }

        footer.layoutParams = params

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_apps)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProductsListScreen())
                .commit()
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> { /* … */ }
                R.id.nav_profile -> { /* … */ }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    // Infla os botões extras na Toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    // Trata clique no hambúrguer e nos botões do header
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            R.id.action_search -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
