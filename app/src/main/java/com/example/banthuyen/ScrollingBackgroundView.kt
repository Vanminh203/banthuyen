package com.example.banthuyen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.scale

class ScrollingBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var background: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.background) // ảnh nền
    private var offsetX = 0f

    init {
        // scale cho khớp chiều cao màn hình
        background =
            background.scale(background.width, height.takeIf { it > 0 } ?: background.height)
    }

    fun setOffset(offset: Float) {
        offsetX = offset % background.width  // lặp vô hạn
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ 2 ảnh liên tiếp
        canvas.drawBitmap(background, -offsetX, 0f, null)
        canvas.drawBitmap(background, background.width - offsetX, 0f, null)
    }
}
