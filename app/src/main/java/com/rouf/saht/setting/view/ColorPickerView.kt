package com.rouf.saht.setting.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorPickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private val bitmapPaint = Paint()
    private val selectorOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.WHITE
        setShadowLayer(4f, 0f, 0f, 0x66000000)
    }
    private val selectorInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.BLACK
    }

    private var selectedX = 0f
    private var selectedY = 0f
    private var currentColor = Color.RED

    var onColorChanged: ((Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            bitmap = createSpectrumBitmap(w, h)
            positionSelectorForColor(currentColor)
        }
    }

    private fun createSpectrumBitmap(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val halfH = h / 2f
        val hsv = floatArrayOf(0f, 1f, 1f)
        for (x in 0 until w) {
            hsv[0] = x * 360f / w
            for (y in 0 until h) {
                if (y <= halfH) {
                    hsv[1] = y / halfH
                    hsv[2] = 1f
                } else {
                    hsv[1] = 1f
                    hsv[2] = 1f - (y - halfH) / halfH
                }
                bmp.setPixel(x, y, Color.HSVToColor(hsv))
            }
        }
        return bmp
    }

    override fun onDraw(canvas: Canvas) {
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, bitmapPaint) }
        canvas.drawCircle(selectedX, selectedY, 14f, selectorOuterPaint)
        canvas.drawCircle(selectedX, selectedY, 14f, selectorInnerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                selectedX = event.x.coerceIn(0f, (width - 1).toFloat())
                selectedY = event.y.coerceIn(0f, (height - 1).toFloat())
                currentColor = bitmap?.getPixel(selectedX.toInt(), selectedY.toInt()) ?: Color.RED
                onColorChanged?.invoke(currentColor)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    fun setColor(color: Int) {
        currentColor = color
        positionSelectorForColor(color)
        invalidate()
    }

    fun getColor(): Int = currentColor

    private fun positionSelectorForColor(color: Int) {
        if (width <= 0 || height <= 0) return
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        selectedX = (hsv[0] / 360f * width).coerceIn(0f, (width - 1).toFloat())
        val halfH = height / 2f
        selectedY = if (hsv[2] < 1f) {
            halfH + (1f - hsv[2]) * halfH
        } else {
            hsv[1] * halfH
        }.coerceIn(0f, (height - 1).toFloat())
    }
}
