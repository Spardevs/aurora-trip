package br.com.ticpass.pos.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

class ConnectionStatusBar(private val context: Context) {
    private var snackbar: Snackbar? = null
    private var currentActivity: Activity? = null
    private val handler = android.os.Handler()
    private val dismissRunnable = Runnable { dismiss() }

    fun attachActivity(activity: Activity) {
        this.currentActivity = activity
    }

    fun detachActivity() {
        this.currentActivity = null
        dismiss()
    }

    fun show(connected: Boolean) {
        currentActivity?.runOnUiThread {
            try {
                val message = if (connected) {
                    "Maquininha conectada à internet"
                } else {
                    "Maquininha sem conexão com a internet"
                }

                val color = if (connected) {
                    ContextCompat.getColor(context, android.R.color.holo_green_dark)
                } else {
                    ContextCompat.getColor(context, android.R.color.holo_red_dark)
                }

                snackbar?.dismiss()

                currentActivity?.let { activity ->
                    val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
                    snackbar = Snackbar.make(rootView, message, 7000)

                    val snackbarView = snackbar?.view
                    snackbarView?.apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.BOTTOM
                            val margin = 8f
                            val scale = context.resources.displayMetrics.density
                            val marginPx = (margin * scale + 0.5f).toInt()
                            setMargins(marginPx, marginPx, marginPx, marginPx)
                        }
                        setPadding(0, 0, 0, 0)

                        findViewById<com.google.android.material.textview.MaterialTextView>(
                            com.google.android.material.R.id.snackbar_text
                        )?.apply {
                            textSize = 11f
                        }
                    }

                    snackbar?.apply {
                        setBackgroundTint(color)
                        setTextColor(Color.WHITE)
                        setAction("Fechar") { dismiss() }
                        setActionTextColor(Color.WHITE)

                        show()

                        handler.removeCallbacks(dismissRunnable)
                        handler.postDelayed(dismissRunnable, 10000)
                    }
                }
            } catch (e: Exception) {
                Log.e("ConnectionStatusBar", "Error showing connection status", e)
            }
        }
    }

    fun dismiss() {
        handler.removeCallbacks(dismissRunnable)
        snackbar?.dismiss()
        snackbar = null
    }
}