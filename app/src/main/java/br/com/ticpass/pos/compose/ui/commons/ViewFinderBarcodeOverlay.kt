package br.com.ticpass.pos.compose.ui.commons

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.max
import kotlin.math.min

class ViewFinderBarcodeOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }

    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }

    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B30")
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
    }

    // Parâmetros do retângulo
    private var aspectW = 4f
    private var aspectH = 3f
    private var horizontalMarginDp = 24f
    private var maxHeightFactor = 0.40f
    private var cornerRadiusDp = 8f
    private var linePaddingDp = 10f

    // Retângulo e animação
    private val cutout = RectF()
    private var scanPosY: Float = 0f
    private var animator: ValueAnimator? = null
    var animationDurationMs: Long = 1600L

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1) Calcula dimensões
        val margin = dp(horizontalMarginDp)
        val radius = dp(cornerRadiusDp)

        val availW = max(w - margin * 2f, dp(120f))
        var rectW = availW
        var rectH = rectW * (aspectH / aspectW)

        val maxH = max(h * maxHeightFactor, dp(120f))
        if (rectH > maxH) {
            rectH = maxH
            rectW = rectH * (aspectW / aspectH)
        }

        var left = (w - rectW) / 2f
        var top = (h - rectH) / 2f - dp(10f)
        var right = left + rectW
        var bottom = top + rectH

        if (top < dp(24f)) {
            val d = dp(24f) - top
            top += d; bottom += d
        }
        if (bottom > h - dp(120f)) {
            val d = bottom - (h - dp(120f))
            top -= d; bottom -= d
        }
        left = max(left, dp(12f))
        right = min(right, w - dp(12f))

        cutout.set(left, top, right, bottom)

        // 2) Máscara + recorte transparente
        val checkpoint = canvas.saveLayer(0f, 0f, w, h, null)
        canvas.drawRect(0f, 0f, w, h, maskPaint)
        val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        canvas.drawRoundRect(cutout, radius, radius, clearPaint)
        canvas.restoreToCount(checkpoint)

        // 3) Borda
        canvas.drawRoundRect(cutout, radius, radius, rectPaint)

        // 4) Linha vermelha animada
        val padding = dp(linePaddingDp)
        val y = scanPosY.coerceIn(cutout.top + padding, cutout.bottom - padding)
        canvas.drawLine(cutout.left + padding, y, cutout.right - padding, y, scanLinePaint)
    }

    private fun ensureAnimator() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDurationMs
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                val padding = dp(linePaddingDp)
                val start = cutout.top + padding
                val end = cutout.bottom - padding
                val t = it.animatedValue as Float
                scanPosY = start + (end - start) * t
                invalidate()
            }
        }
    }

    fun startScan() {
        ensureAnimator()
        if (animator?.isStarted != true) animator?.start()
    }

    fun stopScan() {
        animator?.cancel()
        animator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startScan()
    }

    override fun onDetachedFromWindow() {
        stopScan()
        super.onDetachedFromWindow()
    }

    fun getFramingRect(): RectF = RectF(cutout)

    fun setAspect(width: Float, height: Float) {
        if (width > 0 && height > 0) {
            aspectW = width
            aspectH = height
            invalidate()
        }
    }

    fun setMaxHeightFactor(f: Float) {
        maxHeightFactor = f.coerceIn(0.2f, 0.7f)
        invalidate()
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
}