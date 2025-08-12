package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.ActivitySplitEqualBinding
import com.google.android.material.slider.Slider
import java.text.NumberFormat
import java.util.Locale

class SplitEqualActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplitEqualBinding
    private lateinit var sharedPrefs: SharedPreferences
    private var totalPrice: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplitEqualBinding.inflate(layoutInflater)
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

        sharedPrefs = getSharedPreferences("ShoppingCartPrefs", MODE_PRIVATE)
        totalPrice = getTotalPriceFromPrefs() / 100 // Convertendo de centavos para reais

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvTotalValue.text = formatCurrency(totalPrice)

        setupSlider()
    }

    private fun getTotalPriceFromPrefs(): Double {
        val json = sharedPrefs.getString("shopping_cart_data", null) ?: return 0.0
        return try {
            val totalPriceStr = json.substringAfter("\"totalPrice\":").substringBefore("}")
            totalPriceStr.toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun setupSlider() {
        binding.sliderPeople.addOnChangeListener { _, value, _ ->
            val people = value.toInt()
            binding.tvPeopleCount.text = resources.getQuantityString(
                R.plurals.people_count, people, people
            )

            val dividedValue = totalPrice / people
            binding.tvDividedValue.text = formatCurrency(dividedValue)
        }

        binding.btnConfirm.setOnClickListener {
            val people = binding.sliderPeople.value.toInt()
            val dividedValue = totalPrice / people

            // Retorna o resultado para a tela anterior
            val result = Intent().apply {
                putExtra("divided_value", dividedValue)
                putExtra("people_count", people)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
    }
}