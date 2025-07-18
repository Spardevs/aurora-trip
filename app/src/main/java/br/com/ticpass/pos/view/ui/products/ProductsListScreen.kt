package br.com.ticpass.pos.view.ui.products

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.products.adapter.ProductsAdapter
import br.com.ticpass.pos.viewmodel.products.ProductsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsListScreen : Fragment(R.layout.fragment_products) {
    private val viewModel: ProductsViewModel by viewModels()
    private lateinit var tabLayout: TabLayout
    private lateinit var recycler: RecyclerView
    private val adapter = ProductsAdapter { product ->
        Log.d("ProductsListScreen", "Produto clicado: $product")
        Toast.makeText(
            requireContext(),
            "${product.title} adicionado ao carrinho",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout   = view.findViewById(R.id.tabCategories)
        recycler    = view.findViewById(R.id.rvProducts)
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter       = adapter
        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            tabLayout.removeAllTabs()
            cats.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
            if (cats.isNotEmpty()) onCategorySelected(cats[0])
        }
        viewModel.productsByCategory.observe(viewLifecycleOwner) { map ->
            val pos = tabLayout.selectedTabPosition
            val cat = viewModel.categories.value?.getOrNull(pos)
            adapter.submitList(cat?.let { map[it] } ?: emptyList())
        }
        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                onCategorySelected(tab.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCategoriesWithProducts()
    }

    private fun onCategorySelected(cat: String) {
        adapter.submitList(viewModel.productsByCategory.value?.get(cat) ?: emptyList())
    }
}

