package br.com.ticpass.pos.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
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
                    snackbar = Snackbar.make(rootView, message, 5000)

                    val snackbarView = snackbar?.view
                    snackbarView?.let { view ->
                        // Garanta largura total e remova marginBottom (e margens laterais se desejar)
                        val marginPx = 0 // 0 para ocupar todo o width
                        val topMarginPx = 0 // se quiser um pequeno espaçamento superior, ajuste aqui
                        val params = (view.layoutParams as? ViewGroup.MarginLayoutParams)
                            ?: FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                            )
                        val tv = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)

                        tv?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT
                        params.setMargins(marginPx, topMarginPx, marginPx, 0) // bottom = 0 -> sem marginBottom
                        view.layoutParams = params

                        view.setPadding(0, 0, 0, 0)

                        view.findViewById<com.google.android.material.textview.MaterialTextView>(
                            com.google.android.material.R.id.snackbar_text
                        )?.apply {
                            textSize = 9f
                        }
                    }

                    snackbar?.apply {
                        setBackgroundTint(color)
                        setTextColor(Color.WHITE)
                        setAction("Fechar") { dismiss() }
                        setActionTextColor(Color.WHITE)

                        show()

                        handler.removeCallbacks(dismissRunnable)
                        handler.postDelayed(dismissRunnable, 5000)
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