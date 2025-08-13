package br.com.ticpass.pos.data.activity

import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.MainApplication

open class BaseActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        MainApplication.connectionStatusBar.attachActivity(this)
        MainApplication.connectivityMonitor.checkCurrentStatus()
    }

    override fun onPause() {
        MainApplication.connectionStatusBar.detachActivity()
        super.onPause()
    }
}