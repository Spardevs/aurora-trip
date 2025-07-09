package br.com.ticpass.pos.view.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ConfirmScreen : AppCompatActivity() {

    companion object {
        private const val EXTRA_POS_NAME = "extra_pos_name"

        /**
         * Cria a Intent passando um nome de POS (aqui default “Mock POS”).
         * Você pode chamar ConfirmScreen.newIntent(this, "Minha POS real")
         */
        fun newIntent(context: Context, posName: String = "Mock POS"): Intent {
            return Intent(context, ConfirmScreen::class.java).apply {
                putExtra(EXTRA_POS_NAME, posName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_login)


    }
}
