package br.com.ticpass.pos.view.ui.permissions

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.activity.ProductsActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import br.com.ticpass.pos.view.ui.login.LoginScreen

class PermissionsActivity : AppCompatActivity() {

    // Launcher que vai pedir múltiplas permissões de runtime
    private val permissionsLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { results ->
            val cameraGranted = results[Manifest.permission.CAMERA] == true
            val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (cameraGranted && locationGranted) {
                navigateNext()
            } else {
                showRationaleDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        findViewById<Button>(R.id.btnRequestPermissions).setOnClickListener {
            requestPermissionsIfNeeded()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.CAMERA
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (needed.isEmpty()) {
            // já tem todas
            navigateNext()
        } else {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissões necessárias")
            .setMessage("Para usar este app, você precisa aceitar as permissões de câmera e localização nas configurações.")
            .setPositiveButton("Abrir Configurações") { _, _ ->
                ActivityCompat.startActivityForResult(
                    this,
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.fromParts("package", packageName, null)),
                    0,
                    null
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateNext() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val next = if (!prefs.contains("auth_token")) {
            Intent(this, LoginScreen::class.java)
        } else {
            Intent(this, ProductsActivity::class.java)
        }
        startActivity(next)
        finish()
    }

}
