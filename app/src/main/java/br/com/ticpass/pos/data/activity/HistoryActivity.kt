package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.pass.PassType
import kotlinx.coroutines.launch
import br.com.ticpass.pos.util.getSavedPasses
import br.com.ticpass.pos.util.clearSavedPasses
import java.io.File

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        val btnVoucher = findViewById<Button>(R.id.btnVoucher)
        val btnRefund = findViewById<Button>(R.id.btnRefund)

        // Load and manage saved passes
        lifecycleScope.launch {
            // Example: Get all saved compact passes
            val compactPasses = getSavedPasses(this@HistoryActivity, PassType.ProductCompact)
            val expandedPasses = getSavedPasses(this@HistoryActivity, PassType.ProductExpanded)
            val groupedPasses = getSavedPasses(this@HistoryActivity, PassType.ProductGrouped)

            // You can process these files as needed
            // For example, display them in a list or use them for voucher/refund operations
        }

        // Botão Voucher
        btnVoucher.setOnClickListener {
            val intent = Intent(this, VoucherActivity::class.java)
            startActivity(intent)
        }

        // Botão Refund
        btnRefund.setOnClickListener {
            // Clear saved passes when refunding (example)
            lifecycleScope.launch {
                clearSavedPasses(this@HistoryActivity, PassType.ProductCompact)
                clearSavedPasses(this@HistoryActivity, PassType.ProductExpanded)
                clearSavedPasses(this@HistoryActivity, PassType.ProductGrouped)

                val intent = Intent(this@HistoryActivity, RefundActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Function to get all saved passes (you can move this to a utility class if needed)
    private fun getAllSavedPasses(): List<Pair<PassType, List<File>>> {
        return listOf(
            PassType.ProductCompact to getSavedPasses(this, PassType.ProductCompact),
            PassType.ProductExpanded to getSavedPasses(this, PassType.ProductExpanded),
            PassType.ProductGrouped to getSavedPasses(this, PassType.ProductGrouped)
        )
    }
}