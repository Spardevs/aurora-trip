package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.login.LoginScreen
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

abstract class DrawerBaseActivity : AppCompatActivity() {
    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_base)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView      = findViewById(R.id.nav_view)
        val toolbar  = findViewById< MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_apps)
        }

        val footer = layoutInflater.inflate(R.layout.nav_drawer_footer, navView, false)
        navView.addView(footer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        ))
        footer.setOnClickListener {
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit { clear() }
            getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit { clear() }
            startActivity(Intent(this, LoginScreen::class.java))
            finish()
        }

        val header = navView.getHeaderView(0)
        val operatorNameTv: TextView = header.findViewById(R.id.operatorName)
        val name = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("operator_name", null)
        operatorNameTv.text = name

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_products    -> openProducts()
                R.id.nav_profile -> openProfile()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            true
        } else super.onOptionsItemSelected(item)

    protected abstract fun openProducts()
    protected abstract fun openProfile()
}
