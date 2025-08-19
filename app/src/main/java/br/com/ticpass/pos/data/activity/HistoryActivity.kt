package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager // Mude para LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.History
import br.com.ticpass.pos.view.ui.history.adapter.HistoryAdapter
import br.com.ticpass.pos.view.ui.history.adapter.ProductModalAdapter
import br.com.ticpass.pos.viewmodel.history.HistoryViewModel

class HistoryActivity : AppCompatActivity() {

    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        setupRecyclerView()
        loadHistory()
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.rvHistory)

        // MANTENHA LinearLayoutManager para a lista principal (1 coluna)
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager

        adapter = HistoryAdapter(emptyList()) { history ->
            showHistoryActionsModal(history)
        }

        recyclerView.adapter = adapter
    }

    private fun loadHistory() {
        val historyList = viewModel.getHistory()
        adapter = HistoryAdapter(historyList) { history ->
            showHistoryActionsModal(history)
        }
        findViewById<RecyclerView>(R.id.rvHistory).adapter = adapter
    }

    private fun showHistoryActionsModal(history: History) {
        val dialogView = layoutInflater.inflate(R.layout.modal_history_actions, null)

        val recyclerView: RecyclerView = dialogView.findViewById(R.id.rvProducts)
        val gridLayoutManager = GridLayoutManager(this, 3)
        recyclerView.layoutManager = gridLayoutManager

        val productAdapter = ProductModalAdapter(history.products)
        recyclerView.adapter = productAdapter

        val dialog = AlertDialog.Builder(this)
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
}