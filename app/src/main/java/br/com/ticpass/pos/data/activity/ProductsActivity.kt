package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.products.ProductsListScreen
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartScreen
import javax.inject.Inject

@AndroidEntryPoint
class ProductsActivity : DrawerBaseActivity() {
    private lateinit var cartBadge: TextView
    private lateinit var cartMenuItem: MenuItem

    @Inject
    lateinit var shoppingCartManager: ShoppingCartManager

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

        cartMenuItem = menu.findItem(R.id.fab)
        val badgeLayout = LayoutInflater.from(this).inflate(R.layout.cart_badge, null)
        cartBadge = badgeLayout.findViewById(R.id.cart_badge)
        cartMenuItem.actionView = badgeLayout

        badgeLayout.setOnClickListener {
            val intent = Intent(this, ShoppingCartScreen::class.java)
            startActivityForResult(intent, ProductsListScreen.REQUEST_CART_UPDATE)
        }

        updateCartBadge()

        shoppingCartManager.cartUpdates.observe(this) {
            updateCartBadge()
        }

        return true
    }

    private fun updateCartBadge() {
        val count = shoppingCartManager.getTotalItemsCount()
        if (count > 0) {
            cartBadge.text = count.toString()
            cartBadge.visibility = View.VISIBLE
        } else {
            cartBadge.visibility = View.GONE
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ProductsListScreen.REQUEST_CART_UPDATE) {
            updateCartBadge()
        }
    }

    override fun openProducts() {
        startActivity(Intent(this, ProductsActivity::class.java))
    }

    override fun openHistory() {
         startActivity(Intent(this, HistoryActivity::class.java))
    }
}