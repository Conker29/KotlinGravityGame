package com.epn.gravitygame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class Obstacle(
    var x: Float,
    var y: Float,
    val width: Float = 160f,
    val height: Float = 60f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(239, 68, 68) // rojo
    }

    fun rect(): RectF = RectF(x, y, x + width, y + height)

    fun draw(canvas: Canvas) {
        canvas.drawRoundRect(rect(), 16f, 16f, paint)
    }
}