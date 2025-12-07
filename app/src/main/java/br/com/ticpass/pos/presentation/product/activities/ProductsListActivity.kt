package br.com.ticpass.pos.presentation.product.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.presentation.product.adapters.ProductAdapter
import br.com.ticpass.pos.presentation.product.viewmodels.ProductViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.shared.activities.BaseDrawerActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsListActivity : BaseDrawerActivity() {
    override val hasMenu: Boolean = true

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products_list)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        adapter = ProductAdapter(this, emptyList())
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter

        // Observar os produtos e atualizar o adapter
        lifecycleScope.launch {
            viewModel.products.collectLatest { products ->
                adapter.updateProducts(products)
            }
        }

        // Carregar os produtos ao iniciar
        viewModel.loadProducts()
    }
    override fun showLogoInDrawerToolbar(): Boolean = true

}