package br.com.ticpass.pos.data.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.saveVoucherAsBitmap

class VoucherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.printer_voucher)

        saveVoucherAsBitmap(this)
    }
}
