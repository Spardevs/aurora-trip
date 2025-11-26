package br.com.ticpass.pos.presentation.scanners.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.com.ticpass.pos.R
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView

class QrScannerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_TEXT = "extra_qr_text"
        private const val TAG = "QrScannerActivity"
    }

    private lateinit var barcodeView: BarcodeView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startScanner() else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (result == null) return
            // evitar múltiplos disparos
            barcodeView.pause()
            Log.d(TAG, "QR lido: ${result.text}")
            val intent = Intent().apply { putExtra(EXTRA_QR_TEXT, result.text) }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.zxing_qr_scanner)

        // Solicitar permissão de câmera em runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startScanner()
        }
    }

    private fun startScanner() {
        barcodeView.decodeContinuous(barcodeCallback)
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        try { barcodeView.pauseAndWait() } catch (e: Exception) {}
        super.onDestroy()
    }
}