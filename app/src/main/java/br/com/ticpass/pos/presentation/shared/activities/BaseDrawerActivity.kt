package br.com.ticpass.pos.presentation.shared.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.product.activities.ProductsListActivity
import com.google.android.material.appbar.MaterialToolbar


abstract class BaseDrawerActivity : AppCompatActivity() {

    // Flag para controlar o modo (com menu ou sem menu)
    protected open val hasMenu: Boolean = true

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var noMenuLayout: LinearLayout

    private lateinit var drawerToolbar: MaterialToolbar
    private lateinit var noMenuToolbar: MaterialToolbar

    private lateinit var drawerContentFrame: FrameLayout
    private lateinit var noMenuContentFrame: FrameLayout

    private lateinit var toolbarLogo: ImageView
    private lateinit var toolbarCenterTitle: TextView
    private lateinit var toolbarTextTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_base)

        drawerLayout = findViewById(R.id.drawer_layout)
        noMenuLayout = findViewById(R.id.no_menu_layout)

        drawerToolbar = findViewById(R.id.drawer_toolbar)
        noMenuToolbar = findViewById(R.id.no_menu_toolbar)

        drawerContentFrame = findViewById(R.id.content_frame)
        noMenuContentFrame = findViewById(R.id.no_menu_content_frame)

        toolbarLogo = findViewById(R.id.toolbar_logo)
        toolbarCenterTitle = findViewById(R.id.toolbar_center_title)
        toolbarTextTitle = findViewById(R.id.toolbar_text_title)

        if (hasMenu) {
            // Mostrar layout com menu
            drawerLayout.visibility = View.VISIBLE
            noMenuLayout.visibility = View.GONE

            setupDrawerToolbar()
        } else {
            // Mostrar layout sem menu
            drawerLayout.visibility = View.GONE
            noMenuLayout.visibility = View.VISIBLE

            setupNoMenuToolbar()
        }
    }

    private fun setupDrawerToolbar() {
        // Exibir título ou logo conforme necessidade
        if (showLogoInDrawerToolbar()) {
            toolbarLogo.visibility = View.VISIBLE
            toolbarCenterTitle.visibility = View.GONE
        } else {
            toolbarLogo.visibility = View.GONE
            toolbarCenterTitle.visibility = View.VISIBLE
            toolbarCenterTitle.text = getDrawerToolbarTitle()
        }

        // Configurar ícone do menu para abrir drawer
        drawerToolbar.setNavigationIcon(R.drawable.ic_apps)
        drawerToolbar.navigationIcon?.let { drawable ->
            DrawableCompat.setTint(drawable, resources.getColor(android.R.color.white, theme))
        }
        drawerToolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNoMenuToolbar() {
        // Exibir título no toolbar sem menu
        toolbarTextTitle.text = getNoMenuToolbarTitle()

        // Configurar ícone para abrir nova tela
        noMenuToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        noMenuToolbar.navigationIcon?.let { drawable ->
            DrawableCompat.setTint(drawable, resources.getColor(android.R.color.white, theme))
        }
        noMenuToolbar.setNavigationOnClickListener {
            onNoMenuIconClicked()
        }
    }

    protected fun setContentFragment(fragment: Fragment) {
        val frameId = if (hasMenu) R.id.content_frame else R.id.no_menu_content_frame
        supportFragmentManager.beginTransaction()
            .replace(frameId, fragment)
            .commit()
    }

    // Override para definir se mostra logo ou título no drawer toolbar
    protected open fun showLogoInDrawerToolbar(): Boolean = false

    // Override para definir título no drawer toolbar
    protected open fun getDrawerToolbarTitle(): String = ""

    // Override para definir título no no-menu toolbar
    protected open fun getNoMenuToolbarTitle(): String = ""

    // Override para definir ação ao clicar no ícone no modo sem menu
    protected open fun onNoMenuIconClicked() {
        val intent = Intent(this, ProductsListActivity::class.java)
        startActivity(intent)
    }
}