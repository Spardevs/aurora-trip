package br.com.ticpass.pos.data.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
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
import androidx.core.graphics.drawable.DrawableCompat
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

    object SessionPrefs {
        private const val PREFS_NAME = "SessionPrefs"
        private const val KEY_SESSION_VALID_UNTIL = "session_valid_until"
        private const val DEFAULT_SESSION_DURATION_MIN = 1

        @SuppressLint("UseKtx")
        fun startSession(context: Context, durationMinutes: Int = DEFAULT_SESSION_DURATION_MIN) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val validUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
            prefs.edit { putLong(KEY_SESSION_VALID_UNTIL, validUntil)}
        }

        fun isSessionValid(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val validUntil = prefs.getLong(KEY_SESSION_VALID_UNTIL, 0)
            return System.currentTimeMillis() < validUntil
        }

        @SuppressLint("UseKtx")
        fun clearSession(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_SESSION_VALID_UNTIL).apply()
        }
    }
}

@AndroidEntryPoint
abstract class DrawerBaseActivity : BaseActivity() {
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
        val toolbar  = findViewById<MaterialToolbar>(R.id.drawer_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.navigationIcon = null

//        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_apps)?.mutate()
//        drawable?.let {
//            DrawableCompat.setTint(it, ContextCompat.getColor(this, R.color.colorWhite))
//            supportActionBar?.setHomeAsUpIndicator(it)
//        }

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
                R.id.nav_passes    -> openPasses()
                R.id.nav_history -> openHistory()
                R.id.nav_report -> openReport()
                R.id.nav_withdrawal -> openWithdrawal()
                R.id.nav_support -> openSupport()
                R.id.nav_settings -> openSettings()
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
        if (ShoppingCart.SessionPrefs.isSessionValid(this)) {
            drawerLayout.openDrawer(GravityCompat.START)
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auth_method, null)
        val methodPanel = dialogView.findViewById<View>(R.id.method_panel)
        val passwordPanel = dialogView.findViewById<View>(R.id.password_panel)
        val passwordInput = dialogView.findViewById<EditText>(R.id.password_input)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Acesso ao Menu")
            .setNegativeButton("Voltar") { _, _ -> }
            .create()

        methodPanel.visibility = View.VISIBLE
        passwordPanel.visibility = View.GONE

        dialogView.findViewById<View>(R.id.btn_qr_code).setOnClickListener {
            dialog.dismiss()
            launchQrScanner()
        }

        dialogView.findViewById<View>(R.id.btn_password).setOnClickListener {
            methodPanel.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    methodPanel.visibility = View.GONE
                    passwordPanel.visibility = View.VISIBLE
                    passwordPanel.alpha = 0f
                    passwordPanel.animate().alpha(1f).setDuration(300).start()
                    passwordInput.requestFocus()
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.VISIBLE
                }.start()
        }

        dialogView.findViewById<View>(R.id.btn_confirm_password).setOnClickListener {
            val password = passwordInput.text.toString()
            if (password == "1337") {
                // Inicia a sessão ao validar a senha
                ShoppingCart.SessionPrefs.startSession(this, 1)
                dialog.dismiss()
                drawerLayout.openDrawer(GravityCompat.START)
            } else {
                passwordInput.error = "Senha incorreta"
                passwordInput.text.clear()
            }
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener {
                passwordPanel.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        passwordPanel.visibility = View.GONE
                        methodPanel.visibility = View.VISIBLE
                        methodPanel.alpha = 0f
                        methodPanel.animate().alpha(1f).setDuration(300).start()
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE
                    }.start()
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                ContextCompat.getColor(this, R.color.cardview_dark_background))
        }

        dialog.show()
    }

    private fun handleQrResult(result: ActivityResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                val authResponse = result.data?.getStringExtra("auth_response")
                if (authResponse != null) {
                    ShoppingCart.SessionPrefs.startSession(this, 1)
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
//            android.R.id.home -> {
//                showConfirmationDialog()
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected abstract fun openProducts()
    protected abstract fun openHistory()
    protected abstract fun openPasses()
    protected abstract fun openReport()
    protected abstract fun openWithdrawal()
    protected abstract fun openSupport()
    protected abstract fun openSettings()

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
