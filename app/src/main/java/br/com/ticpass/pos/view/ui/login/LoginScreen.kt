package br.com.ticpass.pos.view.ui.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.data.activity.QrScannerActivity
import br.com.ticpass.pos.databinding.ViewLoginBinding

class LoginScreen : AppCompatActivity() {
    private lateinit var binding: ViewLoginBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventScreen

    private val scannerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = Intent(this, br.com.ticpass.pos.data.activity.EventActivity::class.java)
            startActivity(intent)
            finish()

        } else {
            val error = result.data?.getStringExtra("auth_error")
            Log.e("LoginScreen", "Erro no login: $error")
            Toast.makeText(this, "Falha no login: $error", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ViewLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.emailLoginButton.setOnClickListener {
            // performEmailLogin()
        }

        binding.qrCodeLoginButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA
                )
            } else {
                startQrScanner()
            }
        }
    }

    private fun startQrScanner() {
        val intent = Intent(this, QrScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startQrScanner()
        } else {
            Log.w("LoginScreen", "Permissão de câmera negada")
            Toast.makeText(this, "Permissão de câmera é necessária", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}