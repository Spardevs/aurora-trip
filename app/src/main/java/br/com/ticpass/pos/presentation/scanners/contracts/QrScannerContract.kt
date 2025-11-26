package br.com.ticpass.pos.presentation.scanners.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import br.com.ticpass.pos.presentation.scanners.activities.QrScannerActivity

class QrScannerContract : ActivityResultContract<Unit, String?>() {
    override fun createIntent(
        context: Context,
        input: Unit
    ): Intent {
        return Intent(context, QrScannerActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.getStringExtra(QrScannerActivity.EXTRA_QR_TEXT)
    }
}