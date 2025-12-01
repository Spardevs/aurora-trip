package br.com.ticpass.pos.presentation.login.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import br.com.ticpass.pos.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import br.com.ticpass.pos.core.util.SessionPrefsManagerUtils
import br.com.ticpass.pos.presentation.shared.activities.BaseActivity

@AndroidEntryPoint
class LoginConfirmActivity : BaseActivity() {
    companion object {
        private const val EXTRA_POS_NAME = "extra_pos_name"
        fun newIntent(context: Context, posName: String = "Mock POS"): Intent {
            return Intent(context, LoginConfirmActivity::class.java)
                .apply { putExtra(EXTRA_POS_NAME, posName) }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_confirm)

        // Inicializa o SessionPrefsManagerUtils com o contexto
        SessionPrefsManagerUtils.init(this)

        val menuName = SessionPrefsManagerUtils.getMenuName() ?: "—"
        val dateStartStr = SessionPrefsManagerUtils.getMenuStartDate()
        val dateEndStr = SessionPrefsManagerUtils.getMenuEndDate()
        val posNamePref = SessionPrefsManagerUtils.getPosName()
        val posNameIntent = intent.getStringExtra(EXTRA_POS_NAME)
        val posName = posNamePref ?: posNameIntent ?: "—"

        val menuValueTv = findViewById<TextView>(R.id.menuValue)
        val dateValueTv = findViewById<TextView>(R.id.dateValue)
        val posValueTv = findViewById<TextView>(R.id.posValue)

        dateValueTv.text = "${formatDate(dateStartStr.toString())} - ${formatDate(dateEndStr.toString())}"
        menuValueTv.text = menuName
        posValueTv.text = posName

        // Configura o listener do botão de confirmação
        val confirmButton = findViewById<ImageButton>(R.id.imageButton)
        confirmButton.setOnClickListener {
            loginFinish()
        }
    }

    private fun loginFinish() {
        val name = findViewById<EditText>(R.id.nameText).text.toString()
        SessionPrefsManagerUtils.saveOperatorName(name)
        val intent = Intent(this, LoadingDownloadFragmentActivity::class.java)
        startActivity(intent)
        finish()
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