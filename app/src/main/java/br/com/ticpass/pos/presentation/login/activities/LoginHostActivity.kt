package br.com.ticpass.pos.presentation.login.activities

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.R
import br.com.ticpass.pos.core.util.DeviceUtils
import br.com.ticpass.pos.presentation.login.fragments.LoginChoiceFragment
import br.com.ticpass.pos.presentation.login.fragments.LoginHostCallback
import dagger.hilt.android.AndroidEntryPoint

/**
 * Host activity for login flow. Contains:
 * - Device info button
 * - Falling skydiver animation
 * - Fragment container for login screens
 * 
 * Delegates actual login logic to fragments:
 * - LoginChoiceFragment: shows login method buttons
 * - CredentialLoginFragment: email/username login form
 */
@AndroidEntryPoint
class LoginHostActivity : AppCompatActivity(), LoginHostCallback {

    private lateinit var fallingImg: ImageView
    private var fallingOriginalTranslationY: Float = 0f
    private val fallingUpOffsetDp = 190

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_host)

        initViews()
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.login_fragment_container, LoginChoiceFragment())
                .commit()
        }
    }

    private fun initViews() {
        // Device info button
        findViewById<ImageView>(R.id.deviceInfo).setOnClickListener { 
            showDeviceInfoDialog() 
        }

        // Falling image setup
        fallingImg = findViewById(R.id.falling)
        fallingImg.isClickable = false
        fallingImg.translationY = 0f
        fallingImg.post { fallingOriginalTranslationY = fallingImg.translationY }
    }

    // LoginHostCallback implementation
    override fun onShowCredentialForm() {
        animateFalling(raise = true)
    }

    override fun onHideCredentialForm() {
        animateFalling(raise = false)
    }

    private fun animateFalling(raise: Boolean) {
        val offset = fallingUpOffsetDp * resources.displayMetrics.density
        val target = if (raise) fallingOriginalTranslationY - offset else fallingOriginalTranslationY
        fallingImg.animate().translationY(target).setDuration(250).start()
    }

    private fun showDeviceInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_device_info, null)
        val tvSerial = dialogView.findViewById<TextView>(R.id.tvSerial)
        val tvAcquirer = dialogView.findViewById<TextView>(R.id.tvAcquirer)
        val tvModel = dialogView.findViewById<TextView>(R.id.tvModel)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

        tvModel.text = DeviceUtils.getDeviceModel()
        tvAcquirer.text = DeviceUtils.getAcquirer()
        tvSerial.text = try { DeviceUtils.getDeviceSerial(this) } catch (e: Exception) { "unknown" }

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        btnOk.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Let fragments handle back navigation
        if (supportFragmentManager.backStackEntryCount > 0) {
            onHideCredentialForm()
            supportFragmentManager.popBackStack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
