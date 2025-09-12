package br.com.ticpass.pos.data.activity

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R


enum class PrinterStatus {
    LOADING,
    SUCCESS,
    ERROR
}

class PrinterProcessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_process)

        // Pega o status enviado pela outra tela
        val status = intent.getSerializableExtra("printer_status") as? PrinterStatus ?: PrinterStatus.LOADING

        // Views
        val panel = findViewById<LinearLayout>(R.id.printerPanel)
        val image = findViewById<ImageView>(R.id.printerImage)
        val title = findViewById<TextView>(R.id.printerTitle)

        when (status) {
            PrinterStatus.LOADING -> {
                image.setImageResource(R.drawable.hand_print_pagseguro)
                title.text = getString(R.string.loading)
                panel.setBackgroundResource(R.drawable.bg_dialog_loading)
            }

            PrinterStatus.SUCCESS -> {
                image.setImageResource(R.drawable.ic_check)
                title.text = getString(R.string.success)
                panel.setBackgroundResource(R.drawable.bg_dialog_success)
            }

            PrinterStatus.ERROR -> {
                image.setImageResource(R.drawable.ic_close)
                title.text = getString(R.string.error)
                panel.setBackgroundResource(R.drawable.bg_dialog_error)
            }
        }
    }
}