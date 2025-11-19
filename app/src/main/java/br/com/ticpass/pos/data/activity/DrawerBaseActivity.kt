package br.com.ticpass.pos.data.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.acquirers.workers.jobs.syncPos
import br.com.ticpass.pos.data.api.ApiRepository
import br.com.ticpass.pos.data.event.ForYouViewModel
import br.com.ticpass.pos.data.room.AppDatabase
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity
import br.com.ticpass.pos.data.room.repository.AcquisitionRepository
import br.com.ticpass.pos.data.room.repository.CashierRepository
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.data.room.repository.EventRepository
import br.com.ticpass.pos.data.room.repository.PosRepository
import br.com.ticpass.pos.data.room.repository.ProductRepository
import br.com.ticpass.pos.feature.printing.PrintingViewModel
import br.com.ticpass.pos.printing.events.PrintingHandler
import br.com.ticpass.pos.queue.models.ProcessingState
import br.com.ticpass.pos.queue.processors.printing.processors.models.PrintingProcessorType
import br.com.ticpass.pos.sdk.AcquirerSdk
import br.com.ticpass.pos.view.fragments.printing.PrintingErrorDialogFragment
import br.com.ticpass.pos.view.fragments.printing.PrintingLoadingDialogFragment
import br.com.ticpass.pos.view.fragments.printing.PrintingSuccessDialogFragment
import br.com.ticpass.pos.view.ui.login.LoginScreen
import br.com.ticpass.pos.view.ui.login.PosScreen
import br.com.ticpass.pos.view.ui.pass.PassData
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
            prefs.edit { putLong(KEY_SESSION_VALID_UNTIL, validUntil) }
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
    @Inject
    lateinit var apiRepository: ApiRepository
    private var isSyncing = false
    private var syncActionView: View? = null
    private var syncProgressBar: ProgressBar? = null
    private var syncTitleView: TextView? = null
    private var syncIconView: ImageView? = null
    private var syncLauncher: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_base)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        drawerLayout.drawerElevation = 0f

        val toolbar = findViewById<MaterialToolbar>(R.id.drawer_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_apps)?.mutate()
        drawable?.let {
            DrawableCompat.setTint(it, ContextCompat.getColor(this, R.color.colorWhite))
            supportActionBar?.setHomeAsUpIndicator(it)
        }

        // Acessar header diretamente pelo id
        val header = findViewById<LinearLayout>(R.id.drawer_header)
        val operatorNameTv: TextView = header.findViewById(R.id.operatorName)
        val name = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("operator_name", null)
        operatorNameTv.text = name

        // Configurar clique no footer logout
        val footerLogout = findViewById<LinearLayout>(R.id.footer_logout)
        setupFooterLogoutListeners(footerLogout)

        setupSyncMenuItem()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_products -> {
                    openProducts()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_passes -> {
                    openPasses()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_history -> {
                    openHistory()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_report -> {
                    openReport()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_withdrawal -> {
                    openWithdrawal()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_support -> {
                    openSupport()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_settings -> {
                    openSettings()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.button_sync -> {
                    if (!isSyncing) startInlineSync()
                }
                R.id.nav_print_last_order -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    printLastOrder()
                    true
                }
                R.id.nav_refund -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    refundOrder()
                    true
                }
                else -> false
            }
            true
        }

        updateHeaderInfo()
    }


    private fun refundOrder() {
        val intent = Intent(this, BarcodeScannerActivity::class.java)
        startActivity(intent)
    }

    private fun printLastOrder() {
        lifecycleScope.launch {
            try {
                val lastAcq: AcquisitionEntity? = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(this@DrawerBaseActivity)
                    val repo = AcquisitionRepository.getInstance(db.acquisitionDao())
                    repo.getLastAcquisition()
                }

                if (lastAcq == null) {
                    runOnUiThread {
                        Toast.makeText(
                            this@DrawerBaseActivity,
                            "Nenhum pedido encontrado para impressão",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val acquisitions: List<AcquisitionEntity> = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(this@DrawerBaseActivity)
                    val repo = AcquisitionRepository.getInstance(db.acquisitionDao())
                    repo.getAllByOrderIdRaw(lastAcq.order)
                }

                if (acquisitions.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@DrawerBaseActivity,
                            "Nenhum item encontrado no último pedido",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val operatorName = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    .getString("operator_name", "Atendente") ?: "Atendente"

                val (posName, eventName, eventDate) = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(this@DrawerBaseActivity)
                    val pos = db.posDao().getAll().firstOrNull { it.isSelected }
                    val event = db.eventDao().getAllEvents().firstOrNull { it.isSelected }
                    Triple(pos?.name ?: "POS", event?.name ?: "ticpass", event?.getFormattedStartDate() ?: "")
                }

                val printTs = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                    .format(Date())

                val passList: List<PassData> =
                    acquisitions.flatMap { acq ->
                        listOf(
                            PassData(
                                header = PassData.HeaderData(
                                    title = eventName,
                                    date = eventDate,
                                    barcode = acq.pass
                                ),
                                productData = PassData.ProductData(
                                    name = acq.name,
                                    price = formatPriceReais(acq.price),
                                    eventTitle = eventName,
                                    eventTime = eventDate
                                ),
                                footer = PassData.FooterData(
                                    cashierName = operatorName,
                                    menuName = posName,
                                    description = "Ficha válida por 15 dias após a emissão...",
                                    printTime = printTs
                                ),
                                showCutLine = true
                            )
                        )
                    }

                if (passList.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@DrawerBaseActivity, "Nada para imprimir", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val passFiles: List<File> = withContext(Dispatchers.IO) {
                    passList.mapNotNull { passData ->
                        br.com.ticpass.pos.util.savePassAsBitmap(
                            context = this@DrawerBaseActivity,
                            passData = passData
                        )
                    }
                }

                if (passFiles.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@DrawerBaseActivity,
                            "Falha ao gerar imagens de impressão",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                AcquirerSdk.initialize(this@DrawerBaseActivity)
                runOnUiThread { showPrintingLoadingDialog() }

                val printingHandler = PrintingHandler(
                    context = this@DrawerBaseActivity,
                    lifecycleOwner = this@DrawerBaseActivity
                )

                passFiles.forEach { file ->
                    printingViewModel.enqueuePrinting(
                        file.absolutePath,
                        PrintingProcessorType.ACQUIRER
                    )
                }

                printingViewModel.startProcessing()

                observePrintingStateForReprint()

            } catch (e: Exception) {
                Log.e("DrawerBaseActivity", "Erro ao imprimir último pedido: ${e.message}", e)
                runOnUiThread {
                    dismissPrintingLoadingDialog()
                    Toast.makeText(
                        this@DrawerBaseActivity,
                        "Erro ao imprimir: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun formatPriceReais(priceInCents: Long): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return nf.format(priceInCents / 100.0)
    }

    private fun getLatestPassBitmap(): Bitmap? {
        return try {
            val dir = File(cacheDir, "printing")
            if (!dir.exists()) return null
            val files = dir.listFiles()?.filter { it.isFile } ?: return null
            val latest = files.maxByOrNull { it.lastModified() } ?: return null
            BitmapFactory.decodeFile(latest.absolutePath)
        } catch (e: Exception) {
            Log.e("DrawerBaseActivity", "Erro ao buscar bitmap: ${e.message}")
            null
        }
    }

    private var printingLoadingDialog: PrintingLoadingDialogFragment? = null
    private var printingSuccessDialog: PrintingSuccessDialogFragment? = null
    private var printingErrorDialog: PrintingErrorDialogFragment? = null

    private fun showPrintingLoadingDialog() {
        if (printingLoadingDialog?.isAdded == true) return
        printingLoadingDialog = PrintingLoadingDialogFragment()
        printingLoadingDialog?.show(supportFragmentManager, "printing_loading_reprint")
    }

    private fun dismissPrintingLoadingDialog() {
        printingLoadingDialog?.dismissAllowingStateLoss()
        printingLoadingDialog = null
    }

    private fun showPrintingSuccessDialog() {
        printingSuccessDialog = PrintingSuccessDialogFragment().apply {
            onFinishListener = object : PrintingSuccessDialogFragment.OnFinishListener {
                override fun onFinish() {
                    dismissAllowingStateLoss()
                }
            }
        }
        printingSuccessDialog?.show(supportFragmentManager, "printing_success_reprint")

        printingSuccessDialog?.dialog?.window?.decorView?.postDelayed({
            printingSuccessDialog?.dismissAllowingStateLoss()
        }, 1200L)
    }

    private fun showPrintingErrorDialog() {
        printingErrorDialog = PrintingErrorDialogFragment()
        printingErrorDialog?.cancelPrintingListener = object : PrintingErrorDialogFragment.OnCancelPrintingListener {
            override fun onCancelPrinting() {
                printingViewModel.cancelAllPrintings()
                printingErrorDialog?.dismissAllowingStateLoss()
            }
        }
        printingErrorDialog?.show(supportFragmentManager, "printing_error_reprint")
    }

    private fun observePrintingStateForReprint() {
        lifecycleScope.launch {
            printingViewModel.processingState.collect { state ->
                when (state) {
                    is ProcessingState.QueueDone<*> -> {
                        dismissPrintingLoadingDialog()
                        showPrintingSuccessDialog()
                    }

                    is ProcessingState.ItemFailed<*> -> {
                        dismissPrintingLoadingDialog()
                        showPrintingErrorDialog()
                    }

                    is ProcessingState.QueueAborted<*>,
                    is ProcessingState.QueueCanceled<*> -> {
                        dismissPrintingLoadingDialog()
                        showPrintingErrorDialog()
                    }

                    is ProcessingState.ItemDone<*> -> {
                    }

                    else -> {
                    }
                }
            }
        }
    }

    private val printingViewModel: PrintingViewModel by lazy {
        ViewModelProvider(this)[PrintingViewModel::class.java]
    }


    private fun setupSyncMenuItem() {
        val syncMenuItem = navView.menu.findItem(R.id.button_sync)
        if (syncMenuItem == null) {
            Log.e("DrawerBaseActivity", "button_sync NÃO encontrado no menu")
            return
        }

        syncActionView = syncMenuItem.actionView
        if (syncActionView == null) {
            Log.e("DrawerBaseActivity", "actionView do button_sync é null. Verifique app:actionLayout no XML.")
            return
        }

        syncProgressBar = syncActionView?.findViewById(R.id.progress)
        syncTitleView = syncActionView?.findViewById(R.id.title)
        syncIconView = syncActionView?.findViewById(R.id.icon)

        if (syncProgressBar == null || syncTitleView == null || syncIconView == null) {
            Log.e("DrawerBaseActivity", "IDs progress/title/icon não encontrados em menu_item_sync.xml")
            return
        }

        syncTitleView?.text = getString(R.string.sync)
        syncIconView?.setImageResource(R.drawable.ic_cloud_sync)

        syncActionView?.isClickable = true
        syncActionView?.isFocusable = true
        syncActionView?.setOnClickListener {
            Log.d("DrawerBaseActivity", "Clique no actionView do Sync")
            if (!isSyncing) startInlineSync()
        }

        Log.d("DrawerBaseActivity", "Item de sync configurado com sucesso")
    }

    protected fun setSyncLauncher(block: () -> Unit) {
        syncLauncher = block
    }

    private fun startInlineSync() {
        if (isSyncing) return
        isSyncing = true
        showSyncUIStartingDeterminate()

        if (syncLauncher != null) {
            syncLauncher?.invoke()
        } else {
            onSyncErrorInternal()
            Toast.makeText(this, "ViewModel não disponível. Configure setSyncLauncher { ... }.", Toast.LENGTH_LONG).show()
        }
    }
    private fun showSyncUIStarting() {
        Log.d("DrawerBaseActivity", "showSyncUIStarting() - mostrando barra")
        syncProgressBar?.apply {
            isIndeterminate = true
            visibility = View.VISIBLE
            alpha = 1f
        }
        syncTitleView?.alpha = 0.6f
        syncIconView?.alpha = 0.6f
        syncActionView?.requestLayout()
        syncActionView?.invalidate()
    }

    protected fun runSyncJob(
        viewModel: ForYouViewModel,
        onStart: () -> Unit = {},
        onProgress: (Int) -> Unit = { _ -> },
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        onStart()

        // Retrieve sessionId from SharedPreferences
        val sessionId = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
            .getString("session_id", null) ?: ""

        if (sessionId.isEmpty()) {
            onError("ID da sessão não encontrado.")
            onSyncErrorInternal()
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    syncPos(
                        forYouViewModel = viewModel,
                        sessionId = sessionId, // Pass sessionId here
                        onProgress = { p ->
                            runOnUiThread {
                                updateSyncProgress(p)
                                onProgress(p)
                            }
                        },
                        onFailure = { cause ->
                            runOnUiThread {
                                onError(cause.ifBlank { "Falha ao sincronizar." })
                                onSyncErrorInternal()
                            }
                        },
                        onDone = {
                            runOnUiThread {
                                onSyncSuccessInternal()
                                onSuccess()
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    onError(e.message ?: "Erro inesperado")
                    onSyncErrorInternal()
                }
            }
        }
    }

    private fun onSyncSuccess() {
        isSyncing = false
        syncProgressBar?.visibility = View.GONE
        syncTitleView?.alpha = 1f
        syncIconView?.alpha = 1f

        Toast.makeText(this, "Sync ok", Toast.LENGTH_SHORT).show()

        val original = syncTitleView?.text
        syncTitleView?.text = "Sync ok"
        syncTitleView?.postDelayed({
            syncTitleView?.text = original
        }, 1200)
    }

    private fun onSyncSuccessInternal() {
        isSyncing = false
        updateSyncProgress(100)
        hideSyncUI()
        Toast.makeText(this, "Sync ok", Toast.LENGTH_SHORT).show()
    }

    private fun onSyncErrorInternal() {
        isSyncing = false
        hideSyncUI()
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
                    } ?: run {
                        menuOpeningTv.text = "Desconhecido"
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFooterLogoutListeners(footerLogout: LinearLayout?) {
        val handler = Handler(Looper.getMainLooper())
        var longPressRunnable: Runnable? = null

        footerLogout?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longPressRunnable = Runnable {
                        showFullLogoutConfirmationDialog()
                    }
                    handler.postDelayed(longPressRunnable!!, 3000)
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    false
                }
                else -> false
            }
        }

        footerLogout?.setOnClickListener {
            lifecycleScope.launch {
                logoutClearDb(false)
                startActivity(Intent(this@DrawerBaseActivity, PosScreen::class.java))
                finish()
            }
        }
    }

    private fun showFullLogoutConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout_confirmation)
        dialog.setCancelable(true)

        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val btnYes = dialog.findViewById<Button>(R.id.btnYes)
        val btnNo = dialog.findViewById<Button>(R.id.btnNo)

        btnYes.setOnClickListener {
            dialog.dismiss()
            lifecycleScope.launch {
                logoutClearDb(true)
                startActivity(Intent(this@DrawerBaseActivity, LoginScreen::class.java))
                finish()
            }
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_access_restricted)
        dialog.setCancelable(true)

        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val title = dialog.findViewById<TextView>(R.id.dialogTitle)
        val message = dialog.findViewById<TextView>(R.id.dialogMessage)
        val qrButton = dialog.findViewById<Button>(R.id.dialogQrButton)
        val closeButton = dialog.findViewById<ImageView>(R.id.dialogCloseButton)

        title.text = "Acesso restrito"
        message.text = "Aproxime seu NFC ou acesse via QR Code"
        qrButton.text = "QR Code"

        qrButton.setOnClickListener {
            dialog.dismiss()
            launchQrScanner()

        }

        closeButton.setOnClickListener {
            dialog.dismiss()
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
            android.R.id.home -> {
                // Abre o drawer diretamente sem pedir QR/NFC
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
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

    suspend fun logoutClearDb(fullLogout: Boolean) {
        try {
            posRepository.clearAll()
            menuRepository.clearAll()
            cashierRepository.clearAll()
            productsRepository.clearAll()
            categoryRepository.clearAll()

            // explicitly call the member function - wrapped so logout continues on failure
            try {
                this.closePos()
            } catch (e: Exception) {
                Log.e("DrawerBaseActivity", "Failed to close POS during logout (ignored): ${e.message}", e)
            }

            if (fullLogout) {
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit { clear() }
                getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit { clear() }
            }

            Log.d("User logout", "Db cleared, user logged out")
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun closePos() {
        try {
            val sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)

            val sessionIdCandidates = listOf("session_id", "pos_session_id", "sessionId", "pos_session", "session")
            val sessionId =
                sessionIdCandidates.firstNotNullOfOrNull { sessionPrefs.getString(it, null) }
                    ?: ""

            val posAccessTokenCandidates = listOf("pos_access_token", "posAccessToken", "pos_token", "posToken", "access_token", "access")
            val posAccessToken =
                posAccessTokenCandidates.firstNotNullOfOrNull { sessionPrefs.getString(it, null) }
                    ?: ""

            // You were previously using auth_token from UserPrefs; keep that as proxyCredentials if appropriate
            val jwt = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("auth_token", null) ?: ""

            if (sessionId.isBlank()) {
                Log.w("DrawerBaseActivity", "Skipping closePos because sessionId is missing")
                return
            }

            // Call the updated ApiRepository method
            apiRepository.closePosSession(
                posAccessToken = posAccessToken,
                proxyCredentials = jwt,
                sessionId = sessionId
            )
        } catch (e: Exception) {
            Log.e("DrawerBaseActivity", "Error closing pos", e)
            throw e
        }
    }

    private fun showSyncUIStartingDeterminate() {
        syncProgressBar?.apply {
            visibility = View.VISIBLE
            isIndeterminate = false
            max = 100
            progress = 0
        }
        syncTitleView?.alpha = 0.6f
        syncIconView?.alpha = 0.6f
    }

    private fun updateSyncProgress(percent: Int) {
        syncProgressBar?.apply {
            if (visibility != View.VISIBLE) visibility = View.VISIBLE
            if (isIndeterminate) isIndeterminate = false
            progress = percent.coerceIn(0, 100)
        }
    }

    private fun hideSyncUI() {
        syncProgressBar?.visibility = View.GONE
        syncTitleView?.alpha = 1f
        syncIconView?.alpha = 1f
    }

}