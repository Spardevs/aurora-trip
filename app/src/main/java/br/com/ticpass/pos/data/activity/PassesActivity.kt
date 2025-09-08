package br.com.ticpass.pos.data.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import br.com.ticpass.pos.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.data.room.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PassesActivity : BaseActivity() {

    private var isPrintingEnabled = true
    private var currentFormat: FormatType = FormatType.DEFAULT

    @Inject
    lateinit var eventRepository: EventRepository

    private lateinit var printerIcon: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var formatIcon: ImageView
    private lateinit var formatTextView: TextView

    private enum class FormatType(val formatNameRes: Int, val iconRes: Int, val formatValue: String) {
        DEFAULT(R.string.format_passes_default, R.drawable.ic_two_passes, "default"),
        COMPACT(R.string.format_passes_compact, R.drawable.ic_two_passes, "compact"),
        GROUPED(R.string.format_passes_grouped, R.drawable.ic_two_passes, "grouped");

        companion object {
            fun fromValue(value: String): FormatType {
                return entries.firstOrNull { it.formatValue == value } ?: DEFAULT
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passes)

        initViews()
        setupClickListeners()
        loadInitialStates()
    }

    private fun initViews() {
        printerIcon = findViewById(R.id.toolbar_icon)
        titleTextView = findViewById(R.id.toolbar_title)
        formatIcon = findViewById(R.id.format_icon)
        formatTextView = findViewById(R.id.format_text_view)
    }

    private fun setupClickListeners() {
        findViewById<ConstraintLayout>(R.id.active_print_passes_toolbar).setOnClickListener {
            isPrintingEnabled = !isPrintingEnabled
            updatePrintingUi()
            savePrintingState(isPrintingEnabled)
        }

        findViewById<ConstraintLayout>(R.id.format_passes_toolbar).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                cycleFormat()
            }
        }
    }

    private fun loadInitialStates() {
        isPrintingEnabled = loadPrintingState()
        updatePrintingUi()

        lifecycleScope.launch {
            try {
                val currEvent = eventRepository.getSelectedEvent()
                if (currEvent == null) {
                    withContext(Dispatchers.Main) {
                        showNoEventSelectedWarning()
                        disableFormatChange()
                    }
                    return@launch
                }

                currentFormat = FormatType.fromValue(currEvent.ticketFormat)
                withContext(Dispatchers.Main) {
                    updateFormatUi()
                }
            } catch (e: Exception) {
                Log.e("PassesActivity", "Error loading initial format", e)
                currentFormat = FormatType.DEFAULT
                withContext(Dispatchers.Main) {
                    updateFormatUi()
                }
            }
        }
    }
    private fun showNoEventSelectedWarning() {
        Toast.makeText(
            this,
            "Nenhum evento selecionado. Selecione um evento primeiro.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun disableFormatChange() {
        findViewById<ConstraintLayout>(R.id.format_passes_toolbar).isEnabled = false
        formatIcon.alpha = 0.5f
        formatTextView.alpha = 0.5f
    }



    private suspend fun cycleFormat() {
        val currEvent = eventRepository.getSelectedEvent() ?: run {
            withContext(Dispatchers.Main) {
                showNoEventSelectedWarning()
            }
            return
        }

        try {
            val newFormat = when (currentFormat) {
                FormatType.DEFAULT -> FormatType.COMPACT
                FormatType.COMPACT -> FormatType.GROUPED
                FormatType.GROUPED -> FormatType.DEFAULT
            }

            currEvent.ticketFormat = newFormat.formatValue
            eventRepository.upsertEvent(currEvent)
            currentFormat = newFormat

            saveFormatState(newFormat)

            withContext(Dispatchers.Main) {
                updateFormatUi()
                Toast.makeText(
                    this@PassesActivity,
                    "Formato alterado para ${getString(newFormat.formatNameRes)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e("PassesActivity", "Error cycling format", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@PassesActivity,
                    "Erro ao alterar formato",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun saveFormatState(format: FormatType) {
        getSharedPreferences("ConfigPrefs", MODE_PRIVATE).edit {
            putString("print_format", format.name)
        }
    }

    private fun loadFormatFromSharedPrefs(): FormatType {
        val formatName = getSharedPreferences("ConfigPrefs", MODE_PRIVATE)
            .getString("print_format", FormatType.DEFAULT.name)
        return try {
            FormatType.valueOf(formatName ?: FormatType.DEFAULT.name)
        } catch (e: Exception) {
            FormatType.DEFAULT
        }
    }

    private fun updatePrintingUi() {
        if (isPrintingEnabled) {
            printerIcon.setImageResource(R.drawable.ic_printer)
            titleTextView.setText(R.string.enabled_printing_passes)
        } else {
            printerIcon.setImageResource(R.drawable.ic_printer_off)
            titleTextView.setText(R.string.disabled_printing_passes)
        }
    }

    private fun updateFormatUi() {
        formatIcon.setImageResource(currentFormat.iconRes)
        formatTextView.text = getString(R.string.format_passes, getString(currentFormat.formatNameRes))
    }

    private fun savePrintingState(enabled: Boolean) {
        getSharedPreferences("ConfigPrefs", MODE_PRIVATE).edit {
            putBoolean("printing_enabled", enabled)
        }
    }

    private fun loadPrintingState(): Boolean {
        return getSharedPreferences("ConfigPrefs", MODE_PRIVATE)
            .getBoolean("printing_enabled", true)
    }
}