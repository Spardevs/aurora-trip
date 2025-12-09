package br.com.ticpass.pos.presentation.product.activities

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.presentation.product.viewmodels.ProductViewModel
import br.com.ticpass.pos.presentation.product.viewmodels.CategoryViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.shared.activities.BaseDrawerActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.adapter.FragmentStateAdapter
import br.com.ticpass.pos.presentation.product.fragments.CategoryProductsFragment
import br.com.ticpass.pos.domain.category.model.Category
import br.com.ticpass.pos.presentation.payment.fragments.PaymentSheetFragment

@AndroidEntryPoint
class ProductsListActivity : BaseDrawerActivity() {
    override val hasMenu: Boolean = true
    private val categoryViewModel: CategoryViewModel by viewModels()
    private lateinit var paymentSheetFragment: PaymentSheetFragment
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    // Adicionar PaymentSheetFragment


    private lateinit var pagerAdapter: CategoryPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = layoutInflater.inflate(R.layout.activity_products_list, null)
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        contentFrame.addView(contentView)

        paymentSheetFragment = PaymentSheetFragment()
        supportFragmentManager.commit {
            replace(R.id.payment_sheet_container, paymentSheetFragment)
        }

        tabLayout = contentView.findViewById(R.id.tabLayout)
        viewPager = contentView.findViewById(R.id.viewPager)

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

    inner class CategoryPagerAdapter(activity: androidx.fragment.app.FragmentActivity) : FragmentStateAdapter(activity) {
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