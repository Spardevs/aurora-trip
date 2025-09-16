package br.com.ticpass.pos.view.ui.products.adapter

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import br.com.ticpass.pos.data.api.Product
import br.com.ticpass.pos.view.fragments.products.ProductsCategoryFragment
import br.com.ticpass.pos.R

class CategoriesPagerAdapter(
    fa: FragmentActivity,
    private val categories: List<String>,
    private val productsByCategory: Map<String, List<Product>>
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        val category = categories[position]
        val products = productsByCategory[category] ?: emptyList()
        return ProductsCategoryFragment.newInstance(category, products)
    }

    fun getCategoryAt(position: Int): String = categories[position]
}