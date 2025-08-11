package br.com.ticpass.pos.data.activity

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.ticpass.pos.databinding.ActivityWithdrawalBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.app.AlertDialog
import br.com.ticpass.pos.view.ui.withdrawal.adapter.WithdrawalHistoryAdapter
import br.com.ticpass.pos.viewmodel.withdrawal.WithdrawalViewModel
import java.text.NumberFormat
import br.com.ticpass.pos.R
import java.util.*

@AndroidEntryPoint
class WithdrawalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWithdrawalBinding
    private val viewModel: WithdrawalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWithdrawalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupButton()

        viewModel.initializeBalance(100_000_000_000.00)
    }

    private fun setupRecyclerView() {
        binding.recyclerViewWithdrawalHistory.apply {
            layoutManager = LinearLayoutManager(this@WithdrawalActivity)
            adapter = WithdrawalHistoryAdapter()
        }
    }

    private fun setupObservers() {
        viewModel.availableBalance.observe(this) { balance ->
            binding.textViewAvailableBalance.text = formatCurrency(balance)
        }
        viewModel.totalWithdrawn.observe(this) { total ->
            binding.textViewTotalWithdrawn.text = formatCurrency(total)
        }
        viewModel.withdrawalHistory.observe(this) { history ->
            (binding.recyclerViewWithdrawalHistory.adapter as WithdrawalHistoryAdapter).submitList(history)
        }
    }

    private fun setupButton() {
        binding.buttonAddWithdrawal.setOnClickListener {
            showAddWithdrawalDialog()
        }
    }

    private fun showAddWithdrawalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_withdrawal, null)
        val editTextAmount = dialogView.findViewById<EditText>(R.id.editTextWithdrawalAmount)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Adicionar Sangria")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val amountText = editTextAmount.text.toString()
                val amount = amountText.replace(",", ".").toDoubleOrNull()
                if (amount != null && amount > 0) {
                    viewModel.addWithdrawal(amount)
                } else {
                    Toast.makeText(this, "Por favor, insira um valor válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()
    }

    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
    }
}