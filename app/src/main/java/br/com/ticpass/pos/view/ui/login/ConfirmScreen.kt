package br.com.ticpass.pos.view.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.viewmodel.login.LoginConfirmViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.data.activity.ProductsActivity
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfirmScreen : AppCompatActivity() {
    private val viewModel: LoginConfirmViewModel by viewModels()

    companion object {
        private const val EXTRA_POS_NAME = "extra_pos_name"
        fun newIntent(context: Context, posName: String = "Mock POS"): Intent {
            return Intent(context, ConfirmScreen::class.java)
                .apply { putExtra(EXTRA_POS_NAME, posName) }
        }
    }
    private lateinit var sessionPref: SharedPreferences
    private lateinit var userPref: SharedPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_login)

        sessionPref = getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        userPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

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
        val name = findViewById<EditText>(R.id.nameText).text.toString()
        userPref.edit { putString("operator_name", name)  }
        userPref.edit { putBoolean("user_logged", true)  }

        lifecycleScope.launch {
            viewModel.insertInfo(sessionPref, userPref)
            startActivity(Intent(this@ConfirmScreen, ProductsActivity::class.java))
            finish()
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM 'às' HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) {
                outputFormat.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
}
