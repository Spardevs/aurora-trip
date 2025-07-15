package br.com.ticpass.pos.data.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.products.fragments.ProductsListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_products)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProductsListFragment())
                .commit()
        }
    }
}
