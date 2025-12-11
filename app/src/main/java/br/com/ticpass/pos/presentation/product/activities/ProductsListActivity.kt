package br.com.ticpass.pos.presentation.product.activities

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import br.com.ticpass.pos.R
import br.com.ticpass.pos.domain.category.model.Category
import br.com.ticpass.pos.presentation.payment.fragments.PaymentSheetFragment
import br.com.ticpass.pos.presentation.product.fragments.CategoryProductsFragment
import br.com.ticpass.pos.presentation.product.viewmodels.CategoryViewModel
import br.com.ticpass.pos.presentation.shared.activities.BaseDrawerActivity
import br.com.ticpass.pos.presentation.shoppingCart.viewmodels.CartViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductsListActivity : BaseDrawerActivity(),
    PaymentSheetFragment.PaymentSheetHeightListener {

    override val hasMenu: Boolean = true
    private val categoryViewModel: CategoryViewModel by viewModels()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private val cartViewModel: CartViewModel by viewModels()
    private lateinit var pagerAdapter: CategoryPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = layoutInflater.inflate(R.layout.activity_products_list, null)
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        contentFrame.addView(contentView)

        tabLayout = contentView.findViewById(R.id.tabLayout)
        viewPager = contentView.findViewById(R.id.viewPager)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.paymentSheetContainer,
                    PaymentSheetFragment() // aqui ele vem compacto
                )
                .commit()
        }

        setupViewPagerAndTabs()
    }

    private fun setupViewPagerAndTabs() {
        pagerAdapter = CategoryPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        lifecycleScope.launch {
            categoryViewModel.uiState.collect { state ->
                when (state) {
                    is br.com.ticpass.pos.presentation.product.states.CategoryUiState.Success -> {
                        pagerAdapter.setCategories(state.categories)
                        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                            tab.text = state.categories[position].name
                        }.attach()
                    }
                    else -> {
                        // Tratamento para Loading ou Error, se necessário
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cartViewModel.reloadCartFromPrefs()
    }

    /** Recebe a altura do PaymentSheet e repassa para os fragments de produtos */
    override fun onPaymentSheetHeightChanged(heightPx: Int) {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment is CategoryProductsFragment) {
                fragment.updateBottomPadding(heightPx)
            }
        }
    }

    inner class CategoryPagerAdapter(activity: FragmentActivity) :
        androidx.viewpager2.adapter.FragmentStateAdapter(activity) {

        private var categories: List<Category> = emptyList()

        override fun getItemCount(): Int = categories.size

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return CategoryProductsFragment.newInstance(categories[position].id)
        }

        fun setCategories(categories: List<Category>) {
            this.categories = categories
            notifyDataSetChanged()
        }
    }

    override fun showLogoInDrawerToolbar(): Boolean = true
}