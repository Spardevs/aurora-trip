package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.History
import br.com.ticpass.pos.data.room.AppDatabase
import br.com.ticpass.pos.data.room.repository.HistoryRepository
import br.com.ticpass.pos.view.ui.history.adapter.HistoryAdapter
import br.com.ticpass.pos.view.ui.history.adapter.ProductModalAdapter
import br.com.ticpass.pos.viewmodel.history.HistoryViewModel
import br.com.ticpass.pos.viewmodel.history.HistoryViewModelFactory

class HistoryActivity : DrawerBaseActivity() {
    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        layoutInflater.inflate(R.layout.activity_history, contentFrame, true)

        // Mostrar título centralizado e ícone de voltar que sempre traz ProductsActivity
        showCenteredTitleWithBack("Histórico")

        setupViewModel()
        setupRecyclerView()
        loadHistory()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getInstance(this) // Assumindo que você tem essa instância
        val repository = HistoryRepository(database.orderDao())
        val factory = HistoryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[HistoryViewModel::class.java]
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.rvHistory)
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager

        adapter = HistoryAdapter(emptyList()) { history ->
            showHistoryActionsModal(history)
        }
        recyclerView.adapter = adapter
    }

    private fun loadHistory() {
        // Observa as mudanças no histórico
        viewModel.histories.observe(this) { historyList ->
            adapter = HistoryAdapter(historyList) { history ->
                showHistoryActionsModal(history)
            }
            findViewById<RecyclerView>(R.id.rvHistory).adapter = adapter
        }

        // Observa o estado de loading (opcional)
        viewModel.isLoading.observe(this) { isLoading ->
            // Aqui você pode mostrar/esconder um loading
        }

        // Carrega os dados
        viewModel.loadHistories()
    }

    private fun showHistoryActionsModal(history: History) {
        val dialogView = layoutInflater.inflate(R.layout.modal_history_actions, null)

        val recyclerView: RecyclerView = dialogView.findViewById(R.id.rvProducts)
        val gridLayoutManager = GridLayoutManager(this, 3)
        recyclerView.layoutManager = gridLayoutManager

        val productAdapter = ProductModalAdapter(history.products)
        recyclerView.adapter = productAdapter

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<View>(R.id.btnVoucher).setOnClickListener {
            val intent = Intent(this, VoucherActivity::class.java).apply {
                putExtra("history_id", history.id)
                putExtra("transaction_id", history.transactionId)
                putExtra("total_amount", history.totalPrice)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnRefund).setOnClickListener {
            val intent = Intent(this, RefundActivity::class.java).apply {
                putExtra("history_id", history.id)
                putExtra("transaction_id", history.transactionId)
                putExtra("amount", history.totalPrice)
                putExtra("payment_method", history.paymentMethod)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun openProducts() {
        // abre a activity que hospeda a lista de produtos
        startActivity(Intent(this, ProductsActivity::class.java))
    }

    override fun openHistory() {
        // já estamos na tela de histórico
    }

    override fun openPasses() {
        startActivity(Intent(this, PassesActivity::class.java))
    }

    override fun openReport() {
        startActivity(Intent(this, ReportActivity::class.java))
    }

    override fun openWithdrawal() {
        startActivity(Intent(this, WithdrawalActivity::class.java))
    }

    override fun openSupport() {
        startActivity(Intent(this, SupportActivity::class.java))
    }

    override fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}