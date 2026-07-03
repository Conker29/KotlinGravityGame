package com.epn.gravitygame

import android.graphics.RectF
import kotlin.math.hypot

object Collision {

    fun circleCircle(c1x: Float, c1y: Float, r1: Float, c2x: Float, c2y: Float, r2: Float): Boolean {
        val distance = hypot((c1x - c2x).toDouble(), (c1y - c2y).toDouble())
        return distance < (r1 + r2)
    }

    fun circleRect(cx: Float, cy: Float, radius: Float, rect: RectF): Boolean {
        val closestX = cx.coerceIn(rect.left, rect.right)
        val closestY = cy.coerceIn(rect.top, rect.bottom)
        val distance = hypot((cx - closestX).toDouble(), (cy - closestY).toDouble())
        return distance < radius
    }

    fun hitEdge(cx: Float, cy: Float, radius: Float, width: Int, height: Int): Boolean {
        return cx - radius <= 0f || cx + radius >= width ||
                cy - radius <= 0f || cy + radius >= height
    }
}