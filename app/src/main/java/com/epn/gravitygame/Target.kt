package com.epn.gravitygame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Target(
    var position: Vector2,
    private val radius: Float = 28f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(34, 197, 94) // verde
    }

    var collected = false

    fun radius(): Float = radius

    fun draw(canvas: Canvas) {
        if (!collected) canvas.drawCircle(position.x, position.y, radius, paint)
    }
}