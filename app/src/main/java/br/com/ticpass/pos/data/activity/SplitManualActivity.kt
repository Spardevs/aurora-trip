package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.databinding.ActivitySplitManualBinding
import java.text.NumberFormat
import java.util.Locale

class SplitManualActivity : BaseActivity() {
    private lateinit var binding: ActivitySplitManualBinding
    private lateinit var sharedPrefs: SharedPreferences
    private var totalPrice: Double = 0.0
    private val minValuePerPerson = 1.0
    private val subtractedValues = mutableListOf<Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplitManualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuração fullscreen
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )

        // Configura a toolbar
        binding.toolbar.setNavigationOnClickListener {
            if (subtractedValues.isNotEmpty()) {
                val result = Intent().apply {
                    putExtra("subtracted_values", subtractedValues.toDoubleArray())
                    putExtra("remaining_value", calculateRemainingValue())
                }
                setResult(RESULT_OK, result)
            }
            finish()
        }

        sharedPrefs = getSharedPreferences("ShoppingCartPrefs", MODE_PRIVATE)
        totalPrice = getTotalPriceFromPrefs() / 100

        binding.tvTotalValue.text = formatCurrency(totalPrice)
        binding.tvRemainingValue.text = formatCurrency(totalPrice)
        setupInput()
        setupButtons()
    }

    private fun setupInput() {
        binding.etValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val valueStr = s?.toString()?.replace(",", ".") ?: ""
                val value = valueStr.toDoubleOrNull() ?: 0.0

                if (value > totalPrice) {
                    binding.textInputLayout.error = "Valor maior que o total"
                    clearCalculations()
                    return
                } else {
                    binding.textInputLayout.error = null
                }

                if (value > 0) {
                    val remainingValue = calculateRemainingValue() - value
                    if (remainingValue >= 0) {
                        binding.tvRemainingValue.text = formatCurrency(remainingValue)
                    } else {
                        binding.textInputLayout.error = "Valor excede o restante"
                    }
                } else {
                    binding.tvRemainingValue.text = formatCurrency(calculateRemainingValue())
                }
            }
        })
    }

    private fun calculateRemainingValue(): Double {
        return totalPrice - subtractedValues.sum()
    }

    private fun clearCalculations() {
        binding.tvRemainingValue.text = formatCurrency(calculateRemainingValue())
    }

    private fun setupButtons() {
        binding.btnConfirm.setOnClickListener {
            val valueStr = binding.etValue.text.toString().replace(",", ".")
            val valueToSubtract = valueStr.toDoubleOrNull() ?: 0.0

            val remainingAfterSubtraction = calculateRemainingValue() - valueToSubtract
            if (remainingAfterSubtraction < 0) {
                binding.etValue.error = "Valor excede o restante"
                return@setOnClickListener
            }

            // Adiciona o valor à lista
            subtractedValues.add(valueToSubtract)

            // Atualiza a UI
            binding.tvRemainingValue.text = formatCurrency(remainingAfterSubtraction)
            binding.etValue.text?.clear()

            // Mostra mensagem de sucesso
            Toast.makeText(
                this,
                "Valor de ${formatCurrency(valueToSubtract)} registrado",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun getTotalPriceFromPrefs(): Double {
        val json = sharedPrefs.getString("shopping_cart_data", null)
        return json?.substringAfter("\"totalPrice\":")?.substringBefore("}")?.toDoubleOrNull() ?: 0.0
    }

    private fun formatCurrency(value: Double): String {
        return try {
            NumberFormat.getCurrencyInstance(Locale("pt", "BR")).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }.format(value)
        } catch (e: Exception) {
            "R$ %.2f".format(value)
        }
    }
}