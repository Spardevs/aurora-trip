package br.com.ticpass.pos.data.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.products.ProductsListScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProductsListScreen())
                .commit()
        }
    }
}
