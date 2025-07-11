package br.com.ticpass.pos.view.ui.products

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.view.ui.products.adapter.ProductsAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.inject.Inject

// Data class para a UI
data class Product(
    val name: String,
    val value: BigInteger,
    val photo: String,
    val category: String
)

@AndroidEntryPoint
class ProductsListScreen : AppCompatActivity() {

    @Inject lateinit var apiRepository: APIRepository

    private var categories: List<String> = emptyList()
    private var allProducts: List<Product> = emptyList()
    private lateinit var adapter: ProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_products)

        val recyclerView = findViewById<RecyclerView>(R.id.rvProducts)
            ?: return showErrorAndFinish("RecyclerView não encontrada")
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = ProductsAdapter(emptyList()) { product ->
            Snackbar.make(
                findViewById(android.R.id.content),
                "Clicked: ${product.name}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
        recyclerView.adapter = adapter

        val tabLayout = findViewById<TabLayout>(R.id.tabCategories)
            ?: return showErrorAndFinish("TabLayout não encontrado")

        fetchProducts(tabLayout)
    }

    private fun fetchProducts(tabLayout: TabLayout) {
        val sharedPref = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val userPref   = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val menuId = sharedPref.getString("selected_menu_id", null)
        val jwt     = userPref.getString("auth_token", null)
        Log.d("ProductsListScreen", "Credenciais: $menuId / $jwt")

        if (menuId.isNullOrBlank() || jwt.isNullOrBlank()) {
            showError("Credenciais inválidas")
            return
        }

        lifecycleScope.launch {
            try {
                val resp = apiRepository.getEventProducts(event = menuId, jwt = jwt)
                if (resp.status == 200) {

                    val categoryNames = resp.result.map { it.name }
                    categories = listOf("Todos") + categoryNames

                    allProducts = resp.result.flatMap { cat ->
                        cat.products.map { p ->
                            Product(
                                name = p.title,
                                photo = p.photo,
                                value = p.value,
                                category = cat.name
                            )
                        }
                    }

                    populateTabs(tabLayout)
                    adapter.updateList(allProducts)

                } else {
                    showError("Erro ${resp.status}: ${resp.message}")
                }
            } catch (e: Exception) {
                Log.e("ProductsListScreen", "API error", e)
                showError("Falha na requisição")
            }
        }
    }

    private fun populateTabs(tabLayout: TabLayout) {
        tabLayout.removeAllTabs()
        categories.forEach { name ->
            tabLayout.addTab(tabLayout.newTab().setText(name))
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val sel = tab.text.toString()
                val filtered = if (sel == "Todos") allProducts
                else allProducts.filter { it.category == sel }
                adapter.updateList(filtered)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showError(msg: String) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
    }

    private fun showErrorAndFinish(msg: String) {
        showError(msg)
        finish()
    }
}
