package br.com.ticpass.pos.view.ui.products

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.FragmentProductsBinding
import br.com.ticpass.pos.view.ui.products.adapter.ProductsAdapter
import br.com.ticpass.pos.viewmodel.products.ProductsViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsListScreen : Fragment(R.layout.fragment_products) {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductsViewModel by viewModels()
    private lateinit var adapter: ProductsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductsBinding.bind(view)

        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = ProductsAdapter(emptyList()) { product ->
            Toast.makeText(requireContext(), "Clicado: ${product.title}", Toast.LENGTH_SHORT).show()
        }
        binding.rvProducts.adapter = adapter

        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            binding.tabCategories.removeAllTabs()
            cats.forEach { name ->
                binding.tabCategories.addTab(binding.tabCategories.newTab().setText(name))
            }
            cats.firstOrNull()?.let(::updateProductList)
        }

        viewModel.productsByCategory.observe(viewLifecycleOwner) {
            val pos = binding.tabCategories.selectedTabPosition
            viewModel.categories.value
                ?.getOrNull(pos)
                ?.let(::updateProductList)
        }

        binding.tabCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.text?.toString()?.let(::updateProductList)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        viewModel.loadCategoriesWithProducts()
    }

    private fun updateProductList(category: String) {
        val list = viewModel.productsByCategory.value?.get(category) ?: emptyList()
        adapter.updateList(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
