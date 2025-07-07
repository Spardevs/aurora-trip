package br.com.ticpass.pos.compose.ui.commons

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ViewFinderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#B3000000") // Preto semi-transparente
        style = Paint.Style.FILL
    }

    private val cutoutPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Cria uma camada com canal alfa
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // Pinta o fundo escuro
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Define o retângulo central
        val rectSize = width * 0.7f
        val left = (width - rectSize) / 2
        val top = (height - rectSize) / 2
        val right = left + rectSize
        val bottom = top + rectSize
        val rect = RectF(left, top, right, bottom)

        // Recorta a área central (transparente)
        canvas.drawRect(rect, cutoutPaint)

        // Desenha a borda branca
        canvas.drawRect(rect, borderPaint)

        canvas.restoreToCount(layerId)
    }
}
