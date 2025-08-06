package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.room.AppDatabase
import br.com.ticpass.pos.data.room.repository.CashierRepository
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.data.room.repository.EventRepository
import br.com.ticpass.pos.data.room.repository.PosRepository
import br.com.ticpass.pos.data.room.repository.ProductRepository
import br.com.ticpass.pos.view.ui.login.LoginScreen
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ShoppingCart(
    val items: Map<String, Int>,
    val observations: Map<String, String>,
    val totalPrice: Long
) {
    companion object {
        fun fromJson(json: String): ShoppingCart? {
            return try {
                Gson().fromJson(json, ShoppingCart::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@AndroidEntryPoint
abstract class DrawerBaseActivity : AppCompatActivity() {
    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navView: NavigationView

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> handleQrResult(result) }
    @Inject
    lateinit var posRepository: PosRepository
    @Inject
    lateinit var menuRepository: EventRepository
    @Inject
    lateinit var cashierRepository: CashierRepository
    @Inject
    lateinit var productsRepository: ProductRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_base)

        drawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        drawerLayout.drawerElevation = 0f
        navView = findViewById(R.id.nav_view)

        val db = AppDatabase.getInstance(this)

        posRepository = PosRepository(db.posDao())
        menuRepository = EventRepository(db.eventDao())
        cashierRepository = CashierRepository(db.cashierDao())
        productsRepository = ProductRepository(db.productDao())
        categoryRepository = CategoryRepository(db.categoryDao())

        navView      = findViewById(R.id.nav_view)
        val toolbar  = findViewById< MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_apps)
        }

        val footer = layoutInflater.inflate(R.layout.nav_drawer_footer, navView, false)
        navView.addView(footer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        ))
        footer.setOnClickListener {
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit { clear() }
            getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit { clear() }
            lifecycleScope.launch {
                logoutClearDb()
                startActivity(Intent(this@DrawerBaseActivity, LoginScreen::class.java))
                finish()
            }
        }

        val header = navView.getHeaderView(0)
        val operatorNameTv: TextView = header.findViewById(R.id.operatorName)
        val name = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("operator_name", null)
        operatorNameTv.text = name

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_products    -> openProducts()
                R.id.nav_history -> openHistory()
                R.id.nav_report -> openReport()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        updateHeaderInfo()

    }

    private fun updateHeaderInfo() {
        lifecycleScope.launch {
            try {
                val shoppingCartPrefs = getSharedPreferences("ShoppingCartPrefs", MODE_PRIVATE)
                val cartJson = shoppingCartPrefs.getString("shopping_cart_data", null)
                val shoppingCart = cartJson?.let { json -> ShoppingCart.fromJson(json) }

                val selectedEvent = withContext(Dispatchers.IO) {
                    menuRepository.getSelectedEvent()
                }

                val selectedPos = withContext(Dispatchers.IO) {
                    posRepository.getSelectedPos()
                }

                runOnUiThread {
                    val header = navView.getHeaderView(0)

                    val operatorNameTv: TextView = header.findViewById(R.id.operatorName)
                    val name = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        .getString("operator_name", null)
                    operatorNameTv.text = name

                    val menuOpeningTv: TextView = header.findViewById(R.id.menuOpening)
                    selectedPos.let { pos ->
                        menuOpeningTv.text = if (pos.isClosed) {
                            "Fechado"
                        } else {
                            SimpleDateFormat("EEEE, MMM - HH:mm", Locale("pt", "BR"))
                                .format(Date())
                        }
                    }

                    val menuTotalTv: TextView = header.findViewById(R.id.menuTotal)
                    shoppingCart?.let { cart ->
                        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                        menuTotalTv.text = format.format(cart.totalPrice / 100.0)
                    }

                    val eventLogoTv: ImageView = header.findViewById(R.id.eventLogo)
                    selectedEvent?.let { event ->
                        Glide.with(this@DrawerBaseActivity)
                            .load(event.logo)
                            .placeholder(R.drawable.pix)
                            .into(eventLogoTv)
                    }
                }
            } catch (e: Exception) {
                Log.e("DrawerBaseActivity", "Erro ao carregar informações do header", e)
            }
        }
    }

    private fun showConfirmationDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Acesso ao Menu")
            .setMessage("Deseja acessar o menu administrativo?")
            .setPositiveButton("Sim") { _, _ -> launchQrScanner() }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.design_default_color_primary))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.cardview_dark_background))
        }

        dialog.show()
    }

    private fun handleQrResult(result: ActivityResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                val authResponse = result.data?.getStringExtra("auth_response")
                if (authResponse != null) {
                    drawerLayout.openDrawer(GravityCompat.START)
                } else {
                    Toast.makeText(this, "Autenticação falhou", Toast.LENGTH_SHORT).show()
                }
            }
            RESULT_CANCELED -> {
                val error = result.data?.getStringExtra("auth_error")
                Toast.makeText(this, error ?: "Operação cancelada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchQrScanner() {
        val intent = Intent(this, QrScannerActivity::class.java)
        qrScannerLauncher.launch(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                showConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected abstract fun openProducts()
    protected abstract fun openHistory()
    protected abstract fun openReport()

   suspend fun logoutClearDb() {
       try {
           posRepository.clearAll()
           menuRepository.clearAll()
           cashierRepository.clearAll()
           productsRepository.clearAll()
           categoryRepository.clearAll()

           Log.d("User logout", "Db cleared, user logged out")
       } catch (e: Exception) {
           throw e
       }
    }
}
