package br.com.ticpass.pos.data.activity

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.databinding.ActivityConfigMenuProductsBinding
import br.com.ticpass.pos.view.ui.settings.adapter.ProductConfigAdapter
import br.com.ticpass.pos.viewmodel.settings.ConfigMenuProductsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfigMenuProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigMenuProductsBinding
    private val viewModel: ConfigMenuProductsViewModel by viewModels()
    private lateinit var adapter: ProductConfigAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigMenuProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductConfigAdapter { product ->
            viewModel.toggleProductStatus(product)
        }

        val spanCount = 3
        val layoutManager = GridLayoutManager(this, spanCount)

        binding.productsRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = this@ConfigMenuProductsActivity.adapter
            addItemDecoration(SpacingItemDecoration(4))
        }
    }

    class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = spacing
            outRect.right = spacing
            outRect.bottom = spacing
            outRect.top = spacing
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.products.collectLatest { products ->
                adapter.submitList(products)
            }
        }

        lifecycleScope.launch {
            viewModel.loadingItemId.collectLatest { loadingItemId ->
                adapter.setLoadingItemId(loadingItemId)
            }
        }
    }
}