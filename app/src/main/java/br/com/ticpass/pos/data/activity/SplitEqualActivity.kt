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
    private val paymentsQueue = mutableListOf<Double>()
    private var currentIndex = 0

    companion object {
        const val REQUEST_PAYMENT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplitEqualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("ShoppingCartPrefs", MODE_PRIVATE)
        totalPrice = getTotalPriceFromPrefs() / 100

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvTotalValue.text = formatCurrency(totalPrice)

        setupNumberPicker()
    }

    private fun setupNumberPicker() {
        binding.numberPeople.minValue = 2
        binding.numberPeople.maxValue = 10
        binding.numberPeople.wrapSelectorWheel = false
        binding.numberPeople.value = 2
        updateDividedValue(2)

        binding.numberPeople.setOnValueChangedListener { _, _, newVal ->
            updateDividedValue(newVal)
        }

        binding.btnConfirm.setOnClickListener {
            val people = binding.numberPeople.value
            val dividedValue = totalPrice / people

            paymentsQueue.clear()
            repeat(people) { paymentsQueue.add(dividedValue) }
            currentIndex = 0
            startNextPayment()
        }
    }
    private fun startNextPayment() {
        if (currentIndex < paymentsQueue.size) {
            val intent = Intent(this, PaymentSelectionActivity::class.java).apply {
                putExtra("value_to_pay", paymentsQueue[currentIndex])
                putExtra("total_value", totalPrice)
                putExtra("remaining_value", paymentsQueue[currentIndex])
                putExtra("is_multi_payment", true)
                putExtra("progress", "${currentIndex + 1}/${paymentsQueue.size}")
            }
            startActivityForResult(intent, REQUEST_PAYMENT)
        } else {
            setResult(RESULT_OK)
            finish()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PAYMENT && resultCode == RESULT_OK) {
            currentIndex++
            startNextPayment()
        }
    }

    private fun updateDividedValue(people: Int) {
        binding.tvPeopleCount.text = resources.getQuantityString(
            R.plurals.people_count, people, people
        )
        val dividedValue = totalPrice / people
        binding.tvDividedValue.text = formatCurrency(dividedValue)
    }

    private fun getTotalPriceFromPrefs(): Double {
        val json = sharedPrefs.getString("shopping_cart_data", null) ?: return 0.0
        return json.substringAfter("\"totalPrice\":").substringBefore("}").toDoubleOrNull() ?: 0.0
    }

    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
    }
}
