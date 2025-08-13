package br.com.ticpass.pos.data.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.saveRefundAsBitmap

class RefundActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.printer_refund)

        saveRefundAsBitmap(this)
    }
}