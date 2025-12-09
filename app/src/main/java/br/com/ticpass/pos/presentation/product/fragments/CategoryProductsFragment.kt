// CategoryProductsFragment.kt
package br.com.ticpass.pos.presentation.product.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.payment.fragments.PaymentSheetFragment
import br.com.ticpass.pos.presentation.product.adapters.ProductAdapter
import br.com.ticpass.pos.presentation.product.viewmodels.ProductViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryProductsFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ProductViewModel.Factory

    private val productViewModel: ProductViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return viewModelFactory.create(categoryId) as T
            }
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var paymentSheetFragment: PaymentSheetFragment
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var categoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString(ARG_CATEGORY_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category_products, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewCategoryProducts)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = ProductAdapter(requireContext(), emptyList())
        recyclerView.adapter = adapter



        // Habilitar pull to refresh apenas para a aba "Todos"
        swipeRefreshLayout.isEnabled = (categoryId == "all" || categoryId == null)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar o listener do pull to refresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        lifecycleScope.launch {
            productViewModel.products.collectLatest { products ->
                adapter.updateProducts(products)
                // Parar o indicador de refresh quando os dados forem carregados
                swipeRefreshLayout.isRefreshing = false
            }
        }

        lifecycleScope.launch {
            // Observar mudanças nos produtos (que podem afetar o carrinho)
            productViewModel.products.collectLatest { _ ->
                // Removido updateCartDisplay pois PaymentSheetFragment não é mais filho
            }
        }

    }

    private fun refreshData() {
        // Disparar o refresh no ViewModel apenas para a aba "Todos"
        if (categoryId == "all" || categoryId == null) {
            productViewModel.refreshProducts()
        }
    }

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"

        @JvmStatic
        fun newInstance(categoryId: String) = CategoryProductsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CATEGORY_ID, categoryId)
            }
        }
    }
}