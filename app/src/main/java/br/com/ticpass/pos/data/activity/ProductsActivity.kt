package br.com.ticpass.pos.data.activity

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.databinding.ActivityProductsBinding
import br.com.ticpass.pos.view.ui.products.ProductsListScreen

@AndroidEntryPoint
class ProductsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, ProductsListScreen())
                .commit()
        }
    }
}

