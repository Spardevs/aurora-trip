package br.com.ticpass.pos.view.ui.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import br.com.ticpass.pos.R
import com.google.android.material.snackbar.Snackbar


class QrScannerActivity : AppCompatActivity(), BarcodeCallback {

    private lateinit var barcodeView: DecoratedBarcodeView

    companion object {
        private const val REQUEST_CAMERA = 1001
        private val FORMATS = listOf(BarcodeFormat.QR_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.zxing_barcode_scanner)
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(FORMATS)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        barcodeView.decodeContinuous(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            Log.w("QrScannerActivity", "Permissão de câmera negada")
            finish()
        }
    }

    override fun barcodeResult(result: BarcodeResult) {
        result.text?.let { text ->
            barcodeView.pause()
            if (isPatternMatch(text)) {
                val hashedQr = getHash(text)

                Log.d("QrScannerActivity", "QR com Hash: $text")
                Log.d("QrScannerActivity", "QR sem Hash: $hashedQr")
            } else {
                showInvalidQrError()
            }
        }
    }

    override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) { }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    private fun isPatternMatch(input: String): Boolean {
        val pattern = Regex("^[0-9a-fA-F-]+@[0-9a-zA-Z]+\$")
        return pattern.matches(input)
    }

    fun getHash(input: String): String {
        val atIndex = input.indexOf("@")
        return input.substring(0, atIndex)
    }

    private fun showInvalidQrError() {
        Snackbar.make(barcodeView, "QR inválido: formato não reconhecido", Snackbar.LENGTH_SHORT).show()
    }


}
