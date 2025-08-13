package br.com.ticpass.pos.data.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.databinding.ActivityReportBinding
import br.com.ticpass.pos.viewmodel.report.ReportUiState
import br.com.ticpass.pos.viewmodel.report.ReportViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportActivity : BaseActivity() {
    private lateinit var binding: ActivityReportBinding
    private val viewModel: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnGenerateReport.setOnClickListener {
            viewModel.generateReport(this)
        }
    }

    private fun updateUI(state: ReportUiState) {
        // Progresso
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.btnGenerateReport.isEnabled = !state.isLoading


        // Pré-visualização
        state.previewBitmap?.let { bitmap ->
            binding.imgPreview.visibility = View.VISIBLE
            binding.imgPreview.setImageBitmap(bitmap)
        } ?: run {
            binding.imgPreview.visibility = View.GONE
        }

    }
}