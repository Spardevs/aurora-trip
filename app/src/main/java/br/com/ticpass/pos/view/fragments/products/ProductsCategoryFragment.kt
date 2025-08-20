package br.com.ticpass.pos.view.fragments.products

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.Product
import br.com.ticpass.pos.view.ui.products.adapter.ProductsAdapter
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductsCategoryFragment : Fragment(R.layout.fragment_products_category) {

    @Inject lateinit var shoppingCartManager: ShoppingCartManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductsAdapter

    private var category: String = ""
    private var products: List<Product> = emptyList()

    companion object {
        private const val ARG_CATEGORY = "category"
        private const val ARG_PRODUCTS = "products"

        fun newInstance(category: String, products: List<Product>): ProductsCategoryFragment {
            val args = Bundle().apply {
                putString(ARG_CATEGORY, category)
                putSerializable(ARG_PRODUCTS, ArrayList(products))
            }
            return ProductsCategoryFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            category = it.getString(ARG_CATEGORY) ?: ""
            @Suppress("UNCHECKED_CAST")
            products = (it.getSerializable(ARG_PRODUCTS) as? ArrayList<Product>) ?: emptyList()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvProductsCategory)
        adapter = ProductsAdapter(shoppingCartManager) { product ->
            shoppingCartManager.addItem(product.id)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.adapter = adapter

        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing_small)
        recyclerView.addItemDecoration(ShoppingCartScreen.GridSpacingItemDecoration(3, spacing, true))

        adapter.submitList(products)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.adapter = null
        adapter.clearAll()
    }
}