package br.com.ticpass.pos.presentation.login.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import br.com.ticpass.pos.R
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.presentation.shared.activities.BaseActivity
import br.com.ticpass.pos.data.local.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginPermissionsActivity : BaseActivity() {
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
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null)),
                    0,
                    null
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateNext() {
        // Verifica no banco se já existe um usuário salvo.
        // Se existir, segue para MenuActivity; caso contrário, vai para LoginHostActivity.
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                try {
                    AppDatabase.getDatabase(this@LoginPermissionsActivity).userDao().getAnyUserOnce()
                } catch (e: Exception) {
                    // Em caso de erro ao acessar DB, tratamos como "não autenticado".
                    null
                }
            }

            val next =
//            if (user != null) {
//                Intent(this@PermissionsLoginActivity, MenuActivity::class.java)
//            } else {
                Intent(this@LoginPermissionsActivity, LoginHostActivity::class.java)
//            }
            startActivity(next)
            finish()
        }
    }
}