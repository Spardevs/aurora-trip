package br.com.ticpass.pos.view.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.products.ProductsListScreen
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ConfirmScreen : AppCompatActivity() {

    companion object {
        private const val EXTRA_POS_NAME = "extra_pos_name"
        fun newIntent(context: Context, posName: String = "Mock POS"): Intent {
            return Intent(context, ConfirmScreen::class.java)
                .apply { putExtra(EXTRA_POS_NAME, posName) }
        }
    }
    private lateinit var sessionPref: SharedPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_login)

        sessionPref = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)

        val menuName      = sessionPref.getString("selected_menu_name", "—")
        val dateStartStr  = sessionPref.getString("selected_menu_dateStart", null)
        val dateEndStr    = sessionPref.getString("selected_menu_dateEnd", null)
        val posNamePref   = sessionPref.getString("pos_name", null)
        val posNameIntent = intent.getStringExtra(EXTRA_POS_NAME)
        val posName       = posNamePref ?: posNameIntent ?: "—"

        val menuValueTv = findViewById<TextView>(R.id.menuValue)
        val dateValueTv = findViewById<TextView>(R.id.dateValue)
        val posValueTv  = findViewById<TextView>(R.id.posValue)

        dateValueTv.text = "${formatDate(dateStartStr.toString())} - ${formatDate(dateEndStr.toString())}"

        menuValueTv.text = menuName
        posValueTv.text = posName
    }


    fun loginFinish(view: View) {
        val nameText = findViewById<EditText>(R.id.nameText)
        val name = nameText.text.toString()

        val userPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(userPref.edit()) {
            putString("user_name", name)
            apply()
        }
        // aqui você pode disparar a próxima Activity:
         startActivity(Intent(this, ProductsListScreen::class.java))
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM 'às' HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }
}
