package br.com.ticpass.pos.view.ui.products.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.products.adapter.ProductsAdapter
import br.com.ticpass.pos.viewmodel.products.ProductsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import br.com.ticpass.pos.data.api.Product

@AndroidEntryPoint
class ProductsListFragment : Fragment(R.layout.view_products) {
    private val viewModel: ProductsViewModel by viewModels()
    private lateinit var adapter: ProductsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById< RecyclerView>(R.id.rvProducts)
        rv.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = ProductsAdapter(emptyList()) { product ->
            Snackbar.make(requireView(), "Clicked: ${product.name}", Snackbar.LENGTH_SHORT).show()
        }
        rv.adapter = adapter

        val tabs = view.findViewById< TabLayout>(R.id.tabCategories)

        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            tabs.removeAllTabs()
            cats.forEach { tabs.addTab(tabs.newTab().setText(it)) }
        }
//        viewModel.filteredProducts.observe(viewLifecycleOwner) { list ->
//            adapter.updateList(list)
//        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()
        }

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.filterByCategory(tab.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewModel.fetchProducts()
    }
}
