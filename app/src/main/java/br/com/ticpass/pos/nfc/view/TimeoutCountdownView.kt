package br.com.ticpass.pos.nfc.view

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import br.com.ticpass.pos.R
import java.util.concurrent.TimeUnit

/**
 * Custom view for displaying a countdown timer for input request timeouts
 */
class TimeoutCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val progressBar: ProgressBar
    private val countdownText: TextView
    private var countDownTimer: CountDownTimer? = null
    private var timeoutCallback: (() -> Unit)? = null
    
    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_timeout_countdown, this, true)
        progressBar = root.findViewById(R.id.timeout_progress)
        countdownText = root.findViewById(R.id.timeout_text)
        
        // Default to invisible
        visibility = View.GONE
    }
    
    /**
     * Start the countdown timer
     * @param timeoutMs The timeout duration in milliseconds
     * @param onTimeout Callback to be invoked when the timeout occurs
     */
    fun startCountdown(timeoutMs: Long?, onTimeout: (() -> Unit)? = null) {
        // If no timeout is specified, don't show the countdown
        if (timeoutMs == null || timeoutMs <= 0) {
            android.util.Log.d("TimeoutDebug", "TimeoutCountdownView - No valid timeout, hiding view")
            visibility = View.GONE
            return
        }
        
        // Store the callback
        timeoutCallback = onTimeout
        
        // Cancel any existing timer
        countDownTimer?.cancel()
        
        // Set up the progress bar
        progressBar.max = timeoutMs.toInt()
        progressBar.progress = timeoutMs.toInt()
        
        // Make the view visible
        android.util.Log.d("TimeoutDebug", "TimeoutCountdownView - Making view VISIBLE")
        visibility = View.VISIBLE
        
        // Create and start a new timer
        countDownTimer = object : CountDownTimer(timeoutMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                // Update progress bar
                progressBar.progress = millisUntilFinished.toInt()
                
                // Format and display the remaining time
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                
                val timeText = if (minutes > 0) {
                    String.format("%d:%02d", minutes, seconds)
                } else {
                    String.format("%d", seconds)
                }
                
                countdownText.text = timeText
            }
            
            override fun onFinish() {
                // Hide the view
                android.util.Log.d("TimeoutDebug", "TimeoutCountdownView - onFinish, hiding view")
                visibility = View.GONE
                
                // Invoke the callback if provided
                android.util.Log.d("TimeoutDebug", "TimeoutCountdownView - invoking timeout callback")
                timeoutCallback?.invoke()
            }
        }.start()
    }
    
    /**
     * Cancel the countdown timer
     */
    fun cancelCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
        visibility = View.GONE
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Clean up the timer when the view is detached
        countDownTimer?.cancel()
        countDownTimer = null
    }
}
