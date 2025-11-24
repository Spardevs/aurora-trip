package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.event.ForYouViewModel
import br.com.ticpass.pos.view.ui.products.ProductsListScreen
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import javax.inject.Inject

@AndroidEntryPoint
class ProductsActivity : DrawerBaseActivity() {
    private var cartBadge: TextView? = null
    private lateinit var cartMenuItem: MenuItem

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

    // Obter o ForYouViewModel via Hilt
    private val forYouViewModel: ForYouViewModel by viewModels()

    private val cartUpdatesObserver = Observer<Any> {
        updateCartBadge()
    }
    private var observerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSyncLauncher {
            runSyncJob(
                viewModel = forYouViewModel,
                onStart = { /* já acendemos a barra no Base */ },
                onProgress = { /* opcional: atualizar algo local */ },
                onSuccess = { /* opcional: algo após "Sync ok" */ },
                onError = { cause -> Toast.makeText(this, cause, Toast.LENGTH_LONG).show() }
            )
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, ProductsListScreen())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // Mostrar logo no header da tela de Produtos e exibir hamburger para abrir o drawer
        showToolbarLogo(true, showHamburger = true)

        shoppingCartManager.cartUpdates.observe(this, cartUpdatesObserver)
        updateCartBadge()
    }

    override fun onPause() {
        super.onPause()
        shoppingCartManager.cartUpdates.removeObserver(cartUpdatesObserver)
    }

    private fun updateCartBadge() {
        val count = shoppingCartManager.getTotalItemsCount()
        cartBadge?.let { badge ->
            if (count > 0) {
                badge.text = count.toString()
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ProductsListScreen.REQUEST_CART_UPDATE) {
            updateCartBadge()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observerId?.let {
            shoppingCartManager.removeSafeObserver(it)
        }
        observerId = null
    }

    override fun openProducts() {
        // já estamos aqui; trazer a activity existente (se necessário)
        startActivity(Intent(this, ProductsActivity::class.java))
        finish()
    }

    override fun openHistory() {
        startActivity(Intent(this, HistoryActivity::class.java))
        finish()
    }

    override fun openReport() {
        startActivity(Intent(this, ReportActivity::class.java))
    }

    override fun openPasses() {
        startActivity(Intent(this, PassesActivity::class.java))
    }

    override fun openWithdrawal() {
        startActivity(Intent(this, WithdrawalActivity::class.java))
    }

    override fun openSupport() {
        startActivity(Intent(this, SupportActivity::class.java))
    }

    override fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}